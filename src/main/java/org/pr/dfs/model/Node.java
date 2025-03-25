package org.pr.dfs.model;

import lombok.Data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

@Data
public class Node implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());
    private static final long serialVersionUID = 1L;

    private final String nodeId;
    private final String address;
    private final int port;
    private boolean healthy;
    private long lastHeartbeat;
    private long availableDiskSpace;
    private Set<String> hostedFiles;
    private long startTime;

    public Node(String address, int port) {
        this.nodeId = UUID.randomUUID().toString();
        this.address = address;
        this.port = port;
        this.healthy = true;
        this.lastHeartbeat = System.currentTimeMillis();
        this.availableDiskSpace = getDiskSpace();
        this.hostedFiles = new HashSet<>();
        this.startTime = System.currentTimeMillis();
    }

    public boolean isHealthy() {
        return healthy && System.currentTimeMillis() - lastHeartbeat <= 60000; // 60 second timeout
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public boolean transferFile(String filePath, byte[] data) throws Exception {
        Path localFilePath = Paths.get(filePath);
        try {
            // Ensure the directory exists
            Files.createDirectories(localFilePath.getParent());

            // Write data to the file
            try (FileOutputStream fos = new FileOutputStream(localFilePath.toFile())) {
                fos.write(data);
            }

            // Add file to hosted files
            hostedFiles.add(filePath);

            LOGGER.info("File transferred successfully to " + filePath + " on node " + nodeId);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Error transferring file: " + filePath + " to node: " + nodeId + ": " + e.getMessage());
            return false;
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

    public boolean deleteFile(String filePath) throws Exception {
        Path localFilePath = Paths.get(filePath);
        try {
            Files.deleteIfExists(localFilePath);

            // Remove file from hosted files
            hostedFiles.remove(filePath);

            LOGGER.info("File deleted successfully from " + filePath + " on node " + nodeId);
            return true;
        } catch (Exception e) {
            LOGGER.severe("Error deleting file: " + filePath + " from node: " + nodeId + ": " + e.getMessage());
            return false;
        }
    }

    private long getDiskSpace() {
        try {
            Path path = Paths.get(".");
            return Files.getFileStore(path).getUsableSpace();
        } catch (IOException e) {
            LOGGER.warning("Failed to get disk space: " + e.getMessage());
            return 0;
        }
    }

}