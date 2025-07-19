package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.dto.FileMetadata;
import org.pr.dfs.dto.FileUploadRequest;
import org.pr.dfs.model.*;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.replication.ReplicationManager;
import org.pr.dfs.service.*;
import org.pr.dfs.utils.FileUtils;
import org.pr.dfs.versioning.VersionManager;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final DfsConfig dfsConfig;
    private final NodeManager nodeManager;
    private final ReplicationManager replicationManager;
    private final VersionManager versionManager;
    private final UserService userService;
    private final SimpleNodeService simpleNodeService;
    private final SearchService searchService;
    private final EncryptionService encryptionService;

    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    @Override
    public FileMetaDataDto uploadFile(FileUploadRequest request) throws Exception {
        User currentUser = validateUserContext();

        MultipartFile file = request.getFile();
        String targetDirectory = normalizeDirectory(request.getTargetDirectory());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() <= 0) {
            throw new IllegalArgumentException("Invalid file size");
        }

        if (currentUser.getCurrentUsage() + file.getSize() > currentUser.getQuotaLimit()) {
            throw new IllegalArgumentException("Upload would exceed user quota. Current: " +
                    formatBytes(currentUser.getCurrentUsage()) + ", Limit: " +
                    formatBytes(currentUser.getQuotaLimit()) + ", File size: " + formatBytes(file.getSize()));
        }

        String userDirectory = currentUser.getUserDirectory();
        String fileName = file.getOriginalFilename();

        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String userScopedPath = getUserScopedPath(currentUser, targetDirectory, fileName);

        try {
            Path userDirPath = Paths.get(dfsConfig.getStorage().getPath(), userDirectory, targetDirectory);
            Files.createDirectories(userDirPath);

            log.info("User {} uploading file {} to distributed system (size: {})",
                    currentUser.getUsername(), fileName, formatBytes(file.getSize()));

            Path userFilePath = Paths.get(dfsConfig.getStorage().getPath(), userScopedPath);
            if (Files.exists(userFilePath)) {
                throw new IllegalArgumentException("File already exists: " + fileName);
            }

            byte[] originalFileData = file.getBytes();
            byte[] encryptedFileData = encryptionService.encryptFile(originalFileData, currentUser.getUserId());

            FileMetaDataDto result = processDistributedUpload(file, encryptedFileData ,userScopedPath, request.getReplicationFactor());

            FileMetadata fileMetadata = createFileMetadata(file, originalFileData ,userScopedPath, currentUser, request);
            searchService.saveFileMetadata(fileMetadata);

            userService.updateUserStorageUsage(currentUser.getUserId(), file.getSize());

            if (request.isCreateVersion()) {
                try {
                    versionManager.createVersion(userScopedPath, currentUser.getUsername(),
                            request.getComment() != null ? request.getComment() : "File uploaded via API");
                    log.info("Version created for file: {}", userScopedPath);
                } catch (Exception e) {
                    log.warn("Failed to create version for file {}: {}", userScopedPath, e.getMessage());
                }
            }

            return result;

        } catch (Exception e) {
            log.error("Failed to upload file {} for user {}: {}", fileName, currentUser.getUsername(), e.getMessage());
            throw new RuntimeException("File upload failed: " + e.getMessage(), e);
        }
    }

    private FileMetaDataDto processDistributedUpload(MultipartFile file, byte[] encryptedFileData, String userScopedPath, int replicationFactor) throws Exception {
        List<Node> availableNodes = nodeManager.getAllNodes().stream()
                .filter(Node::isHealthy)
                .collect(Collectors.toList());

        if (availableNodes.isEmpty()) {
            throw new IllegalStateException("No healthy storage nodes available for replication");
        }

        int actualReplicationFactor = Math.min(replicationFactor > 0 ? replicationFactor : dfsConfig.getReplication().getFactor(),
                availableNodes.size());

        log.info("Uploading file {} with replication factor {} across {} nodes",
                userScopedPath, actualReplicationFactor, availableNodes.size());

        List<Node> targetNodes = selectTargetNodes(availableNodes, actualReplicationFactor);

        boolean uploadSuccess = false;
        Exception lastException = null;
        byte[] fileData = file.getBytes();

        // Use SimpleNodeService for distributed upload
        for (Node node : targetNodes) {
            try {
                boolean success = simpleNodeService.storeFileOnNode(node, userScopedPath, encryptedFileData);
                if (success) {
                    node.addHostedFile(userScopedPath);
                    log.info("Encrypted File {} replicated to node {} using simple HTTP", userScopedPath, node.getNodeId());
                    uploadSuccess = true;
                } else {
                    log.error("Failed to replicate encrypted file {} to node {} using simple HTTP", userScopedPath, node.getNodeId());
                }
            } catch (Exception e) {
                log.error("Failed to replicate encrypted file {} to node {}: {}", userScopedPath, node.getNodeId(), e.getMessage());
                lastException = e;
            }
        }

        if (!uploadSuccess) {
            throw new RuntimeException("Failed to upload file to any node", lastException);
        }

        // Store locally for backup/metadata purposes
        Path userFilePath = Paths.get(dfsConfig.getStorage().getPath(), userScopedPath);
        Files.createDirectories(userFilePath.getParent());
        Files.write(userFilePath, encryptedFileData);

        replicationManager.replicateFile(userScopedPath, actualReplicationFactor);

        return FileMetaDataDto.builder()
                .name(file.getOriginalFilename())
                .path(userScopedPath)
                .size(file.getSize())
                .contentType(file.getContentType())
                .uploadTime(LocalDateTime.now())
                .replicationFactor(actualReplicationFactor)
                .build();
    }

    private List<Node> selectTargetNodes(List<Node> availableNodes, int replicationFactor) {
        List<Node> selectedNodes = availableNodes.stream()
                .limit(replicationFactor)
                .collect(Collectors.toList());
        return selectedNodes;
    }

    private Path getNodeStoragePath(Node node, String userScopedPath) {
        return Paths.get(dfsConfig.getStorage().getPath(), "..", "storage", node.getNodeId(), userScopedPath);
    }

    @Override
    public Resource downloadFile(String filePath) throws Exception {
        User currentUser = validateUserContext();

        String normalizedPath = normalizePath(filePath);
        String userScopedPath = getUserScopedPath(currentUser, normalizedPath);

        log.info("User {} downloading file: {} (resolved to: {})",
                currentUser.getUsername(), normalizedPath, userScopedPath);

        try {
            searchService.updateFileAccess(userScopedPath);
        } catch (Exception e) {
            log.warn("Failed to update file access tracking: {}", e.getMessage());
        }

        byte[] encryptedFileData = null;
        String fileName = Paths.get(normalizedPath).getFileName().toString();

        encryptedFileData = simpleNodeService.retrieveFile(userScopedPath);
        if (encryptedFileData != null) {
            log.info("Encrypted File {} retrieved from distributed nodes", userScopedPath);
        } else {
            Path fullPath = Paths.get(dfsConfig.getStorage().getPath(), userScopedPath);

            if(!Files.exists(fullPath)) {
                throw new FileNotFoundException("File not found: " + normalizedPath);
            }

            if(!isWithinUserDirectory(currentUser, fullPath)) {
                throw new SecurityException("Access Denied: File is outside user's directory");
            }

            encryptedFileData = Files.readAllBytes(fullPath);
            log.info("Encrypted file {} retrieved from local storage", userScopedPath);
        }

        try {
            byte[] decryptedFileData = encryptionService.decryptFile(encryptedFileData, currentUser.getUserId());
            log.info("File decrypted for user {} - Encrypted size: {}, Decrypted size: {}",
                    currentUser.getUsername(), encryptedFileData.length, decryptedFileData.length);

            return new ByteArrayResource(decryptedFileData) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
        } catch (Exception e) {
            log.error("Failed to decrypt file {} for user {}: {}", userScopedPath, currentUser.getUsername(), e.getMessage());
            throw new RuntimeException("File decryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public FileMetaDataDto getFileMetaData(String filePath) throws Exception {
        User currentUser = validateUserContext();

        String normalizedPath = normalizePath(filePath);
        String userScopedPath = getUserScopedPath(currentUser, normalizedPath);

        log.info("User {} getting file info: {} (resolved to: {})",
                currentUser.getUsername(), normalizedPath, userScopedPath);

        try {
            FileMetadata dbMetadata = searchService.getFileMetadataByPath(userScopedPath);
            if(dbMetadata != null) {
                return convertFileMetadataToDto(dbMetadata);
            }
        } catch(Exception e) {
            log.warn("Failed to get metadata from database, falling back to file system: {}", e.getMessage());
        }

        Path fullPath = Paths.get(dfsConfig.getStorage().getPath(), userScopedPath);

        if (!Files.exists(fullPath)) {
            throw new FileNotFoundException("File not found: " + normalizedPath);
        }

        if (!isWithinUserDirectory(currentUser, fullPath)) {
            throw new SecurityException("Access denied: File is outside user's directory");
        }

        File file = fullPath.toFile();
        return createFileMetadata(file, userScopedPath, 3);
    }

    @Override
    public boolean deleteFile(String filePath) throws Exception {
        User currentUser = validateUserContext();

        String normalizedPath = normalizePath(filePath);
        String userScopedPath = getUserScopedPath(currentUser, normalizedPath);

        log.info("User {} deleting file: {} (resolved to: {})",
                currentUser.getUsername(), normalizedPath, userScopedPath);

        Path fullPath = Paths.get(dfsConfig.getStorage().getPath(), userScopedPath);

        if (!Files.exists(fullPath)) {
            return false;
        }

        if (!isWithinUserDirectory(currentUser, fullPath)) {
            throw new SecurityException("Access denied: File is outside user's directory");
        }

        long fileSize = 0;
        try {
            FileMetadata metadata = searchService.getFileMetadataByPath(userScopedPath);
            if(metadata != null) {
                fileSize = metadata.getFileSize();
            } else {
                fileSize = Files.size(fullPath);
            }
        } catch (Exception e) {
            log.warn("Could not determine file size for deletion: {}", e.getMessage());
            fileSize =Files.size(fullPath);
        }

        boolean deleted = Files.deleteIfExists(fullPath);

        if (deleted) {
            userService.updateUserStorageUsage(currentUser.getUserId(), -fileSize);

            try {
                searchService.deleteFileMetadata(userScopedPath);
            } catch (Exception e) {
                log.warn("Failed to delete metadata from database: {}", e.getMessage());
            }

            try {
                replicationManager.handleFileDeletion(userScopedPath);
            } catch (Exception e) {
                log.warn("Failed to clean up replication for deleted file {}: {}", userScopedPath, e.getMessage());
            }

            log.info("Successfully deleted encrypted file: {}", userScopedPath);
        }

        return deleted;
    }

    private FileMetaDataDto convertFileMetadataToDto(FileMetadata metadata) {
        return FileMetaDataDto.builder()
                .name(metadata.getFileName())
                .path(metadata.getFilePath())
                .size(metadata.getFileSize())
                .contentType(metadata.getContentType())
                .uploadTime(metadata.getUploadTime())
                .lastModified(metadata.getLastModified())
                .checksum(metadata.getChecksum())
                .replicationFactor(metadata.getReplicationFactor())
                .currentReplicas(metadata.getCurrentReplicas())
                .isDirectory(false)
                .build();
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

    private FileMetadata createFileMetadata(MultipartFile file, byte[] originalFileData,String userScopedPath, User currentUser, FileUploadRequest request) {
        String fileId = UUID.randomUUID().toString();

        return FileMetadata.builder()
                .fileId(fileId)
                .fileName(file.getOriginalFilename())
                .filePath(userScopedPath)
                .userId(currentUser.getUserId())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .uploadTime(LocalDateTime.now())
                .lastModified(LocalDateTime.now())
                .lastAccessed(LocalDateTime.now())
                .checksum(encryptionService.calculateFileHash(originalFileData))
                .description(request.getComment())
                .tags(extractTags(request.getComment()))
                .replicationFactor(request.getReplicationFactor())
                .currentReplicas(1)
                .accessCount(0L)
                .isDeleted(false)
                .build();
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

    private String calculateChecksum(MultipartFile file) {
        try {
            byte[] fileBytes = file.getBytes();
            return FileUtils.calculateCheckSum(fileBytes);
        } catch (Exception e) {
            log.warn("Failed to calculate checksum for file: {}", file.getOriginalFilename());
            return null;
        }
    }

    private Set<String> extractTags(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            return new HashSet<>();
        }

        Set<String> tags = new HashSet<>();
        String[] words = comment.split("\\s+");
        for (String word : words) {
            if (word.startsWith("#") && word.length() > 1) {
                tags.add(word.substring(1).toLowerCase());
            }
        }

        return tags;
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

    private User validateUserContext() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found in context");
        }
        return currentUser;
    }

    private String getUserScopedPath(User user, String directory, String fileName) {
        String userDirectory = user.getUserDirectory();
        if (directory == null || directory.trim().isEmpty() || directory.equals("/")) {
            return Paths.get(userDirectory, fileName).toString().replace("\\", "/");
        }
        return Paths.get(userDirectory, directory, fileName).toString().replace("\\", "/");
    }

    private String getUserScopedPath(User user, String filePath) {
        String userDirectory = user.getUserDirectory();
        if (filePath == null || filePath.trim().isEmpty() || filePath.equals("/")) {
            return userDirectory;
        }
        return Paths.get(userDirectory, filePath).toString().replace("\\", "/");
    }

    private boolean isWithinUserDirectory(User user, Path filePath) {
        try {
            Path userDirPath = Paths.get(dfsConfig.getStorage().getPath(), user.getUserDirectory()).toRealPath();
            Path fileRealPath = filePath.toRealPath();
            return fileRealPath.startsWith(userDirPath);
        } catch (Exception e) {
            log.warn("Failed to validate path security for user {}: {}", user.getUsername(), e.getMessage());
            return false;
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

