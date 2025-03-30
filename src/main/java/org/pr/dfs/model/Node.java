package org.pr.dfs.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Node implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(Node.class.getName());

    private String nodeId;
    private String address;
    private int port;
    private boolean isHealthy;
    private long availableDiskSpace;
    private long lastHeartbeat;
    private Set<String> hostedFiles;
    private String storagePath;
    private  long startTime;

    public Node(String address, int port) {
        this.address = address;
        this.port = port;
        this.isHealthy = true;
        this.hostedFiles = new HashSet<>();
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isHealthy() {
        return isHealthy;
    }

    public void setHealthy(boolean healthy) {
        isHealthy = healthy;
    }

    public long getAvailableDiskSpace() {
        return availableDiskSpace;
    }

    public void setAvailableDiskSpace(long availableDiskSpace) {
        this.availableDiskSpace = availableDiskSpace;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public Set<String> getHostedFiles() {
        return hostedFiles;
    }

    public void setHostedFiles(Set<String> hostedFiles) {
        this.hostedFiles = hostedFiles;
    }

    public void addHostedFile(String filePath) {
        if (this.hostedFiles == null) {
            this.hostedFiles = new HashSet<>();
        }
        this.hostedFiles.add(filePath);
    }

    public void removeHostedFile(String filePath) {
        if (this.hostedFiles != null) {
            this.hostedFiles.remove(filePath);
        }
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    // Added methods for file operations

    public boolean transferFile(String filePath, byte[] fileData) {
        try {
            // Ensure storage directory exists
            Path fullPath = getFullStoragePath(filePath);
            Files.createDirectories(fullPath.getParent());

            // Write the file data
            try (FileOutputStream fos = new FileOutputStream(fullPath.toFile())) {
                fos.write(fileData);
                fos.flush();
            }

            // Add to hosted files
            addHostedFile(filePath);
            LOGGER.info("File " + filePath + " transferred to node " + nodeId);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to transfer file " + filePath + " to node " + nodeId, e);
            return false;
        }
    }

    public boolean deleteFile(String filePath) {
        try {
            Path fullPath = getFullStoragePath(filePath);
            boolean deleted = Files.deleteIfExists(fullPath);

            if (deleted) {
                removeHostedFile(filePath);
                LOGGER.info("File " + filePath + " deleted from node " + nodeId);
            } else {
                LOGGER.warning("File " + filePath + " not found on node " + nodeId);
            }

            return deleted;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete file " + filePath + " from node " + nodeId, e);
            return false;
        }
    }

    public boolean hasFile(String filePath) {
        if (hostedFiles != null && hostedFiles.contains(filePath)) {
            // Verify file actually exists on disk
            Path fullPath = getFullStoragePath(filePath);
            return Files.exists(fullPath);
        }
        return false;
    }

    private Path getFullStoragePath(String filePath) {
        // Normalize the file path to use the storage directory
        String relativePath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        return Paths.get(storagePath, relativePath);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Node node = (Node) o;
        return nodeId != null ? nodeId.equals(node.nodeId) : node.nodeId == null;
    }

    @Override
    public int hashCode() {
        return nodeId != null ? nodeId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeId='" + nodeId + '\'' +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", healthy=" + isHealthy +
                ", files=" + (hostedFiles != null ? hostedFiles.size() : 0) +
                '}';
    }

    public void setLastHeartbeat(long lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
        setLastHeartbeat(System.currentTimeMillis());
    }
}