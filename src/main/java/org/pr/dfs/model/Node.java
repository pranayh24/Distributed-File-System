package org.pr.dfs.model;

import lombok.Data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

@Data
public class Node {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private final String nodeId;
    private final String address;
    private final int port;
    private boolean healthy;
    private long lastHeartbeat;

    public Node(String address, int port) {
        this.nodeId = UUID.randomUUID().toString();
        this.address = address;
        this.port = port;
        this.healthy = true;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public boolean isHealthy() {
        return healthy && System.currentTimeMillis() - lastHeartbeat > 60000; // 60 second timeout
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void transferFile(String filePath, byte[] data) throws Exception {
        Path localFilePath = Paths.get(filePath);
        try {
            // Ensure the directory exists
            Files.createDirectories(localFilePath.getParent());

            // Write data to the file
            try (FileOutputStream fos = new FileOutputStream(localFilePath.toFile())) {
                fos.write(data);
            }

            LOGGER.info("File transferred successfully to " + filePath + " on node " + nodeId);

        } catch (Exception e) {
            LOGGER.severe("Error transferring file: " + filePath + " to node: " + nodeId + ": " + e.getMessage());
            throw new Exception("Failed to transfer file to node storage", e);
        }
    }

    public byte[] getFile(String filePath) throws Exception {
        Path localFilePath = Paths.get(filePath);
        try {
            return Files.readAllBytes(localFilePath);
        } catch (Exception e) {
            LOGGER.severe("Error reading file: " + filePath + " from node: " + nodeId + ": " + e.getMessage());
            throw new Exception("Failed to retrieve file from node storage", e);
        }
    }

    public void deleteFile(String filePath) throws Exception {
        Path localFilePath = Paths.get(filePath);
        try {
            Files.deleteIfExists(localFilePath);
            LOGGER.info("File deleted successfully from " + filePath + " on node " + nodeId);
        } catch (Exception e) {
            LOGGER.severe("Error deleting file: " + filePath + " from node: " + nodeId + ": " + e.getMessage());
            throw new Exception("Failed to delete file from node storage", e);
        }
    }
}