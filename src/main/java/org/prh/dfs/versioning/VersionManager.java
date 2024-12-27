package org.prh.dfs.versioning;

import org.prh.dfs.model.Version;
import org.prh.dfs.utils.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class VersionManager {
    private static final Logger LOGGER = Logger.getLogger(VersionManager.class.getName());
    private static final String VERSION_DIR = ".versions";
    private static final int MAX_VERSIONS = 10; // Maximum versions to keep per file

    private final String storagePath;
    private final Path versionDir;
    private final ConcurrentHashMap<String, List<Version>> versionCache;

    public VersionManager(String storagePath) {
        this.storagePath = storagePath;
        this.versionDir = Paths.get(storagePath, VERSION_DIR);
        this.versionCache = new ConcurrentHashMap<>();
        initializeVersionDirectory();
    }

    private void initializeVersionDirectory() {
        try {
            Files.createDirectories(versionDir);
            LOGGER.info("Version directory initiated at: " + versionDir);
        } catch (IOException e) {
            LOGGER.severe("Failed to initialize version directory: " + e.getMessage());
            throw new RuntimeException("Version directory initialization failed", e);
        }
    }

    public Version createVersion(String filePath, String creator, String comment) throws IOException {
        Path originalFile = Paths.get(storagePath, filePath);
        if(!Files.exists(originalFile)) {
            throw new FileNotFoundException("File not found: " + filePath);
        }

        String versionId = generateVersionId(filePath);
        Path versionedFile = getVersionedFilePath(filePath, versionId);

        // Create version directories if they don't exist
        Files.createDirectories(versionedFile.getParent());

        // Copy the current file to version storage
        Files.copy(originalFile, versionedFile, StandardCopyOption.REPLACE_EXISTING);

        // Create version metadata
        Version version = new Version(
                versionId,
                originalFile.getFileName().toString(),
                filePath,
                Files.size(originalFile),
                FileUtils.calculateCheckSum(Files.readAllBytes(originalFile)),
                Instant.now(),
                creator,
                comment
        );

        // Update version cache
        updateVersionCache(filePath, version);

        LOGGER.info("Created new version " + versionId + " for file: " + filePath);
        return version;
    }

    public List<Version> getVersions(String filePath) {
        return versionCache.getOrDefault(filePath, new ArrayList<>());
    }

    public void restoreVersion(String filePath, String versionId) throws IOException {
        Path versionedFile = getVersionedFilePath(filePath, versionId);
        Path originalFile = Paths.get(storagePath, filePath);

        if(!Files.exists(versionedFile)) {
            throw new FileNotFoundException("Version not found: " + versionId);
        }

        // Backup current version before restoring
        createVersion(filePath, "system", "Automatic backup before version restore");

        // Restore the versioned file
        Files.copy(versionedFile, originalFile, StandardCopyOption.REPLACE_EXISTING);
        LOGGER.info("Restored version " + versionId + " for file: " + filePath);
    }

    private synchronized void updateVersionCache(String filePath, Version version) {
        List<Version> versions = versionCache.computeIfAbsent(filePath, k -> new ArrayList<>());
        versions.add(version);

        // Sort versions by creation time, most recent first
        versions.sort((v1,v2) -> v2.getCreatedAt().compareTo(v1.getCreatedAt()));

        // Keep only MAX_VERSIONS versions
        if(versions.size() > MAX_VERSIONS) {
            Version oldVersion = versions.remove(versions.size()-1);
            deleteOldVersion(filePath, oldVersion.getVersionId());
        }
    }

    private void deleteOldVersion(String filePath, String versionId) {
        try {
            Path versionedFile = getVersionedFilePath(filePath, versionId);
            Files.deleteIfExists(versionedFile);

        } catch(IOException e) {
            LOGGER.warning("Failed to delete old version " + versionId + ": " + e.getMessage());
        }
    }

    private Path getVersionedFilePath(String filePath, String versionId) {
        return versionDir.resolve(filePath + "." + versionId);
    }

    private String generateVersionId(String filePath) {
        return UUID.randomUUID().toString();
    }
}
