package org.pr.dfs.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

public class DataStorageManager {
    private static final Logger LOGGER = Logger.getLogger(DataStorageManager.class.getName());
    private final String baseStoragePath;

    public DataStorageManager(String baseStoragePath) {
        this.baseStoragePath = baseStoragePath;
        createStorageDirectory();
    }

    private void createStorageDirectory() {
        try {
            Files.createDirectories(Path.of(baseStoragePath));
            LOGGER.info("Storage directory created: " + baseStoragePath);
        } catch(IOException e) {
            LOGGER.severe("Failed to create storage directory: " + e.getMessage());
            throw new RuntimeException("Failed to initialize storage", e);
        }
    }

    public void storeFile(String nodeId, String fileName, byte[] data) throws IOException {
        Path nodePath = Path.of(baseStoragePath, nodeId);
        Path filePath = nodePath.resolve(fileName);

        Files.createDirectories(nodePath);
        Files.write(filePath, data);
        LOGGER.info("File stored: " + filePath);
    }
    public byte[] retrieveFile(String nodeId, String fileName) throws IOException{
        Path filePath  = Path.of(baseStoragePath, nodeId, fileName);
        return Files.readAllBytes(filePath);
    }

    public boolean deleteFile(String nodeId, String fileName) {
        try {
            Path filePath = Path.of(baseStoragePath, nodeId, fileName);
            return Files.deleteIfExists(filePath);
        } catch(IOException e) {
            LOGGER.warning("Failed to delete file: " + fileName + " from node: " + nodeId);
            return false;
        }
    }

    public boolean fileExists(String nodeId, String fileName) {
        Path filePath = Path.of(baseStoragePath, nodeId, fileName);
        return Files.exists(filePath);
    }
}
