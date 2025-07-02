package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.model.FileMetaData;
import org.pr.dfs.model.User;
import org.pr.dfs.server.DirectoryHandler;
import org.pr.dfs.service.DirectoryService;
import org.pr.dfs.service.UserService;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectoryServiceImpl implements DirectoryService {

    private final DfsConfig dfsConfig;
    private final UserService userService;
    private DirectoryHandler directoryHandler;

    private DirectoryHandler getDirectoryHandler() {
        if (directoryHandler == null) {
            // Always use the base storage path for DirectoryHandler
            directoryHandler = new DirectoryHandler(dfsConfig.getStorage().getPath());
        }
        return directoryHandler;
    }

    @Override
    public List<FileMetaDataDto> listDirectory(String path) throws Exception {
        User currentUser = validateUserContext();

        String normalizedPath = normalizePath(path);
        String userScopedPath = getUserScopedPath(currentUser, normalizedPath);

        log.info("User {} listing directory: {} (resolved to: {})",
                currentUser.getUsername(), normalizedPath, userScopedPath);

        Path userDirPath = Paths.get(dfsConfig.getStorage().getPath(), currentUser.getUserDirectory());
        if (!Files.exists(userDirPath)) {
            Files.createDirectories(userDirPath);
            log.info("Created user directory: {}", userDirPath);
        }

        List<FileMetaData> files = getDirectoryHandler().listDirectory(userScopedPath);

        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean createDirectory(String path) throws Exception {
        User currentUser = validateUserContext();

        String normalizedPath = normalizePath(path);
        String userScopedPath = getUserScopedPath(currentUser, normalizedPath);

        log.info("User {} creating directory: {} (resolved to: {})",
                currentUser.getUsername(), normalizedPath, userScopedPath);

        return getDirectoryHandler().createDirectory(userScopedPath);
    }

    @Override
    public boolean deleteDirectory(String path) throws Exception {
        User currentUser = validateUserContext();

        String normalizedPath = normalizePath(path);
        String userScopedPath = getUserScopedPath(currentUser, normalizedPath);

        if (normalizedPath.equals("/") || normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("Cannot delete user's root directory");
        }

        log.info("User {} deleting directory: {} (resolved to: {})",
                currentUser.getUsername(), normalizedPath, userScopedPath);

        return getDirectoryHandler().deleteDirectory(userScopedPath);
    }

    @Override
    public boolean moveOrRename(String sourcePath, String destinationPath) throws Exception {
        User currentUser = validateUserContext();

        String normalizedSource = normalizePath(sourcePath);
        String normalizedDestination = normalizePath(destinationPath);

        String userScopedSource = getUserScopedPath(currentUser, normalizedSource);
        String userScopedDestination = getUserScopedPath(currentUser, normalizedDestination);

        log.info("User {} moving directory from: {} to: {} (resolved to: {} -> {})",
                currentUser.getUsername(), normalizedSource, normalizedDestination,
                userScopedSource, userScopedDestination);

        return getDirectoryHandler().moveOrRename(userScopedSource, userScopedDestination);
    }

    private User validateUserContext() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found in context");
        }
        return currentUser;
    }

    private String getUserScopedPath(User user, String path) {
        String userDirectory = user.getUserDirectory();
        if (path.equals("/") || path.isEmpty()) {
            return userDirectory;
        }
        return Paths.get(userDirectory, path).toString().replace("\\", "/");
    }

    private String normalizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/";
        }

        String normalized = path.trim().replaceAll("^/+", "").replaceAll("/+$", "");
        return normalized.isEmpty() ? "/" : normalized;
    }

    private FileMetaDataDto convertToDto(FileMetaData fileMetaData) {
        return FileMetaDataDto.builder()
                .name(fileMetaData.getName())
                .path(fileMetaData.getPath())
                .isDirectory(fileMetaData.isDirectory())
                .size(fileMetaData.getSize())
                .lastModified(LocalDateTime.now())
                .build();
    }
}