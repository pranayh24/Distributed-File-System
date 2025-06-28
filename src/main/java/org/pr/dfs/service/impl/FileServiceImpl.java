package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.dto.FileUploadRequest;
import org.pr.dfs.model.*;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.replication.ReplicationManager;
import org.pr.dfs.service.FileService;
import org.pr.dfs.service.UserService;
import org.pr.dfs.utils.FileUtils;
import org.pr.dfs.versioning.VersionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DfsConfig dfsConfig;
    private final NodeManager nodeManager;
    private final ReplicationManager replicationManager;
    private final VersionManager versionManager;
    private final UserService userService;

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    @Override
    public FileMetaDataDto uploadFile(FileUploadRequest request) throws Exception {
        User currentUser = UserContext.getCurrentUser();
        if(currentUser == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        MultipartFile file = request.getFile();
        String targetDirectory = normalizeDirectory(request.getTargetDirectory());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (currentUser.getCurrentUsage() + file.getSize() > currentUser.getQuotaLimit()) {
            throw new IllegalArgumentException("Upload would exceed user quota");
        }

        String userDirectory = currentUser.getUserDirectory();
        String fileName = file.getOriginalFilename();
        String fullPath = Paths.get(userDirectory,targetDirectory, fileName).toString().replace("\\", "/");

        Path targetDirPath = Paths.get(dfsConfig.getStorage().getPath(), userDirectory, targetDirectory);
        Files.createDirectories(targetDirPath);

        Path destinationPath = Paths.get(dfsConfig.getStorage().getPath(), fullPath);

        log.info("Uploading file {} to {}", fileName, destinationPath);

        processFileUpload(file, destinationPath, request.getReplicationFactor());

        userService.updateUserStorageUsage(currentUser.getUserId(), file.getSize());

        if (request.isCreateVersion()) {
            try {
                versionManager.createVersion(fullPath, "system",
                        request.getComment() != null ? request.getComment() : "File uploaded via API");
                log.info("Version created for file: {}", fullPath);
            } catch (Exception e) {
                log.warn("Failed to create version for file {}: {}", fullPath, e.getMessage());
            }
        }

        CompletableFuture.runAsync(() -> {
            try {
                replicationManager.replicateFile(fullPath, request.getReplicationFactor());
                log.info("Replication initiated for file: {}", fullPath);
            } catch (Exception e) {
                log.error("Failed to initiate replication for file {}: {}", fullPath, e.getMessage());
            }
        });

        return createFileMetadata(destinationPath.toFile(), fullPath, request.getReplicationFactor());
    }

    @Override
    public Resource downloadFile(String filePath) throws Exception {
        User currentUser = UserContext.getCurrentUser();
        if(currentUser == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        String userDirectory = currentUser.getUserDirectory();
        String normalizedPath = normalizePath(filePath);
        String fullPath = Paths.get(userDirectory, normalizedPath).toString().replace("\\", "/");

        Path targetPath = Paths.get(dfsConfig.getStorage().getPath(), userDirectory, fullPath);

        log.info("User {} Downloading file: {}", currentUser.getUsername() ,fullPath);

        if (!Files.exists(targetPath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        Path userDirPath = Paths.get(dfsConfig.getStorage().getPath(), userDirectory);
        if (!targetPath.normalize().startsWith(userDirPath.normalize())) {
            throw new SecurityException("Access denied: Cannot access files outside user directory");
        }

        return new FileSystemResource(fullPath);
    }

    @Override
    public FileMetaDataDto getFileMetaData(String filePath) throws Exception {
        String normalizedPath = normalizePath(filePath);
        Path fullPath = Paths.get(dfsConfig.getStorage().getPath(), normalizedPath);

        if (!Files.exists(fullPath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        File file = fullPath.toFile();
        return createFileMetadata(file, normalizedPath, getReplicationFactor(normalizedPath));
    }

    @Override
    public void deleteFile(String filePath) throws Exception {
        String normalizedPath = normalizePath(filePath);
        Path fullPath = Paths.get(dfsConfig.getStorage().getPath(), normalizedPath);

        log.info("Deleting file: {}", fullPath);

        if (!Files.exists(fullPath)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        if (Files.isDirectory(fullPath)) {
            throw new IllegalArgumentException("Cannot delete a directory using file delete operation: " + filePath);
        }

        // Delete from local storage
        Files.delete(fullPath);

        // Handle replication cleanup
        CompletableFuture.runAsync(() -> {
            try {
                replicationManager.handleFileDeletion(normalizedPath);
                log.info("Replication cleanup completed for file: {}", normalizedPath);
            } catch (Exception e) {
                log.error("Failed to cleanup replication for file {}: {}", normalizedPath, e.getMessage());
            }
        });
    }

    private void processFileUpload(MultipartFile file, Path destinationPath, int replicationFactor) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             FileOutputStream fos = new FileOutputStream(destinationPath.toFile())) {

            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkNumber = 0;
            long totalSize = file.getSize();
            long totalChunks = (totalSize + CHUNK_SIZE - 1) / CHUNK_SIZE;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] chunkData = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunkData, 0, bytesRead);

                FileChunk chunk = new FileChunk(
                        destinationPath.toString(),
                        file.getOriginalFilename(),
                        chunkNumber,
                        chunkData,
                        FileUtils.calculateCheckSum(chunkData),
                        totalChunks
                );

                if (!validateChunk(chunk)) {
                    throw new IOException("Chunk validation failed for chunk " + chunkNumber);
                }

                fos.write(chunkData);
                fos.flush();

                chunkNumber++;

                log.debug("Processed chunk {}/{} for file {}", chunkNumber, totalChunks, file.getOriginalFilename());
            }

            log.info("File upload completed: {} ({} chunks)", file.getOriginalFilename(), chunkNumber);
        }
    }

    private boolean validateChunk(FileChunk chunk) {
        String calculatedChecksum = FileUtils.calculateCheckSum(chunk.getData());
        return calculatedChecksum.equals(chunk.getChecksum());
    }

    private FileMetaDataDto createFileMetadata(File file, String relativePath, int replicationFactor) {
        FileMetaDataDto metadata = new FileMetaDataDto();
        metadata.setName(file.getName());
        metadata.setPath(relativePath);
        metadata.setDirectory(file.isDirectory());
        metadata.setSize(file.length());
        metadata.setLastModified(LocalDateTime.ofInstant(
                new Date(file.lastModified()).toInstant(),
                ZoneId.systemDefault()));

        if (!file.isDirectory()) {
            try {
                byte[] fileBytes = Files.readAllBytes(file.toPath());
                metadata.setChecksum(FileUtils.calculateCheckSum(fileBytes));
            } catch (IOException e) {
                log.warn("Failed to calculate checksum for file {}: {}", relativePath, e.getMessage());
            }
        }

        metadata.setReplicationFactor(replicationFactor);
        metadata.setCurrentReplicas(getCurrentReplicas(relativePath));

        return metadata;
    }

    private int getReplicationFactor(String filePath) {
        try {
            ReplicationStatus status = replicationManager.getReplicationStatus(filePath);
            return status != null ? status.getReplicationFactor() : dfsConfig.getReplication().getFactor();
        } catch (Exception e) {
            log.warn("Failed to get replication factor for {}: {}", filePath, e.getMessage());
            return dfsConfig.getReplication().getFactor();
        }
    }

    private int getCurrentReplicas(String filePath) {
        try {
            ReplicationStatus status = replicationManager.getReplicationStatus(filePath);
            return status != null ? status.getCurrentReplicas() : 1;
        } catch (Exception e) {
            log.warn("Failed to get current replicas for {}: {}", filePath, e.getMessage());
            return 1;
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        return path.replaceAll("^/+", "").replace("\\", "/");
    }

    private String normalizeDirectory(String directory) {
        if (directory == null || directory.isEmpty() || directory.equals("/")) {
            return "";
        }
        return directory.replaceAll("^/+", "").replaceAll("/+$", "").replace("\\", "/");
    }
}