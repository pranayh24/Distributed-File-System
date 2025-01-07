package org.pr.dfs.versioning;

import org.pr.dfs.model.Version;
import org.pr.dfs.utils.FileUtils;

import java.io.*;
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
    private static final String VERSION_DB = "versions.db";
    private static final int MAX_VERSIONS = 10; // Maximum versions to keep per file

    private final String storagePath;
    private final Path versionDir;
    private final Path versionDbFile;
    private final ConcurrentHashMap<String, List<Version>> versionCache;

    public VersionManager(String storagePath) {
        this.storagePath = storagePath;
        this.versionDir = Paths.get(storagePath, VERSION_DIR);
        this.versionDbFile = versionDir.resolve(VERSION_DB);
        this.versionCache = new ConcurrentHashMap<>();
        initializeVersionDirectory();
        loadVersions();
    }

    private void initializeVersionDirectory() {
        try {
            Files.createDirectories(versionDir);
            if(!Files.exists(versionDbFile)) {
                Files.createFile(versionDbFile);
            }
            LOGGER.info("Version directory initiated at: " + versionDir);
        } catch (IOException e) {
            LOGGER.severe("Failed to initialize version directory: " + e.getMessage());
            throw new RuntimeException("Version directory initialization failed", e);
        }
    }

    public void loadVersions() {
        try {
            if(Files.exists(versionDbFile) && Files.size(versionDbFile) > 0) {
                try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(versionDbFile.toFile()))) {
                    @SuppressWarnings("unchecked")
                    ConcurrentHashMap<String, List<Version>> loaded = (ConcurrentHashMap<String, List<Version>>) ois.readObject();
                    versionCache.putAll(loaded);
                    LOGGER.info("Loaded " + versionCache.size() + " version entries from disk");
                }
            }
        } catch(Exception e) {
            LOGGER.warning("Could not load versions from disk: " + e.getMessage());
        }
    }

    public Version createVersion(String filePath, String creator, String comment) throws IOException {
        filePath = filePath.replaceAll("^/+","");

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

        // Save to disk
        saveVersions();

        LOGGER.info("Created new version " + versionId + " for file: " + filePath);
        return version;
    }

    public void saveVersions() {
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(versionDbFile.toFile()))){
            oos.writeObject(versionCache);
            LOGGER.info("Saved versions to disk");
        } catch(Exception e) {
            LOGGER.severe("Failed to save versions to disk: " + e.getMessage());
        }
    }

    public List<Version> getVersions(String filePath) {
        filePath = filePath.replaceAll("^/+","");

        List<Version> versions = versionCache.get(filePath);
        LOGGER.info("Retrieving versions for " + filePath + ": found " +
                (versions!=null ? versions.size() : 0) + " versions");
        return versions != null ? new ArrayList<>(versions) : new ArrayList<>();
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
        LOGGER.info("Updated version cache for " + filePath + ". Total versions: " + versions.size());
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
