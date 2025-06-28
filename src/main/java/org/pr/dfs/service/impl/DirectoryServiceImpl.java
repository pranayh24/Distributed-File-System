package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.model.FileMetaData;
import org.pr.dfs.replication.ReplicationManager;
import org.pr.dfs.server.DirectoryHandler;
import org.pr.dfs.service.DirectoryService;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    private final DfsConfig dfsConfig;
    private final ReplicationManager replicationManager;
    private DirectoryHandler directoryHandler;

    private DirectoryHandler getDirectoryHandler() {
        if (directoryHandler == null) {
            directoryHandler = new DirectoryHandler(dfsConfig.getStorage().getPath());
        }
        return directoryHandler;
    }

    @Override
    public List<FileMetaDataDto> listDirectory(String path) throws Exception {
        String normalizedPath = normalizePath(path);

        log.info("Listing directory: {}", normalizedPath);

        List<FileMetaData> files = getDirectoryHandler().listDirectory(normalizedPath);

        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public void createDirectory(String path) throws Exception {
        String normalizedPath = normalizePath(path);

        log.info("Creating directory: {}", normalizedPath);

        boolean created = getDirectoryHandler().createDirectory(normalizedPath);

        if (!created) {
            throw new RuntimeException("Failed to create directory: " + path);
        }
    }

    @Override
    public void deleteDirectory(String path) throws Exception {
        String normalizedPath = normalizePath(path);

        log.info("Deleting directory: {}", normalizedPath);

        Path fullPath = Paths.get(dfsConfig.getStorage().getPath(), normalizedPath);
        if (!Files.exists(fullPath)) {
            throw new FileNotFoundException("Directory not found: " + path);
        }

        if (!Files.isDirectory(fullPath)) {
            throw new IllegalArgumentException("Path is not a directory: " + path);
        }

        boolean deleted = getDirectoryHandler().deleteDirectory(normalizedPath);

        if (!deleted) {
            throw new RuntimeException("Failed to delete directory: " + path);
        }

        // TODO: Handle replication cleanup for files in the directory
        log.info("Directory deleted successfully: {}", normalizedPath);
    }

    @Override
    public void moveOrRename(String sourcePath, String destinationPath) throws Exception {
        String normalizedSource = normalizePath(sourcePath);
        String normalizedDestination = normalizePath(destinationPath);

        log.info("Moving {} to {}", normalizedSource, normalizedDestination);

        Path sourceFullPath = Paths.get(dfsConfig.getStorage().getPath(), normalizedSource);
        if (!Files.exists(sourceFullPath)) {
            throw new FileNotFoundException("Source path not found: " + sourcePath);
        }

        boolean moved = getDirectoryHandler().moveOrRename(normalizedSource, normalizedDestination);

        if (!moved) {
            throw new RuntimeException("Failed to move/rename: " + sourcePath + " to " + destinationPath);
        }

        // TODO: Update replication status for moved files
        log.info("Move/rename completed successfully");
    }

    private FileMetaDataDto convertToDto(FileMetaData fileMetaData) {
        FileMetaDataDto dto = new FileMetaDataDto();
        dto.setName(fileMetaData.getName());
        dto.setPath(fileMetaData.getPath());
        dto.setDirectory(fileMetaData.isDirectory());
        dto.setSize(fileMetaData.getSize());
        dto.setLastModified(LocalDateTime.ofInstant(
                fileMetaData.getLastModified().toInstant(),
                ZoneId.systemDefault()));

        if (!fileMetaData.isDirectory()) {
            try {
                var status = replicationManager.getReplicationStatus(fileMetaData.getPath());
                if (status != null) {
                    dto.setReplicationFactor(status.getReplicationFactor());
                    dto.setCurrentReplicas(status.getCurrentReplicas());
                } else {
                    dto.setReplicationFactor(dfsConfig.getReplication().getFactor());
                    dto.setCurrentReplicas(1);
                }
            } catch (Exception e) {
                log.warn("Failed to get replication info for {}: {}", fileMetaData.getPath(), e.getMessage());
                dto.setReplicationFactor(dfsConfig.getReplication().getFactor());
                dto.setCurrentReplicas(1);
            }
        }

        return dto;
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "";
        }
        return path.replaceAll("^/+", "").replace("\\", "/");
    }
}