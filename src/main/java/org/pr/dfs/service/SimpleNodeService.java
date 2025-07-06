package org.pr.dfs.service;

import org.pr.dfs.model.Node;
import org.pr.dfs.replication.NodeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

@Service
public class SimpleNodeService {

    private static final Logger LOGGER = Logger.getLogger(SimpleNodeService.class.getName());

    @Autowired
    private NodeManager nodeManager;

    private final HttpClient httpClient;
    private final Random random;

    public SimpleNodeService() {
        this.httpClient = HttpClient.newHttpClient();
        this.random = new Random();
    }

    /**
     * Store file on a healthy node using simple HTTP approach
     */
    public boolean storeFile(String filePath, byte[] fileData) {
        List<Node> healthyNodes = nodeManager.getHealthyNodes();

        if (healthyNodes.isEmpty()) {
            LOGGER.warning("No healthy nodes available for file storage");
            return false;
        }

        // Select a random healthy node for storage
        Node targetNode = healthyNodes.get(random.nextInt(healthyNodes.size()));

        return storeFileOnNode(targetNode, filePath, fileData);
    }

    /**
     * Store file on a specific node
     */
    public boolean storeFileOnNode(Node node, String filePath, byte[] fileData) {
        try {
            // URL encode the file path to handle spaces and special characters
            String encodedFilePath = java.net.URLEncoder.encode(filePath, "UTF-8");
            String url = String.format("http://%s:%d/node/files?filePath=%s",
                node.getAddress(), node.getPort(), encodedFilePath);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileData))
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("File stored successfully on node " + node.getNodeId() + ": " + filePath);
                return true;
            } else {
                LOGGER.warning("Failed to store file on node " + node.getNodeId() + ": " + response.statusCode());
                return false;
            }

        } catch (Exception e) {
            LOGGER.severe("Error storing file on node " + node.getNodeId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieve file from any available node
     */
    public byte[] retrieveFile(String filePath) {
        List<Node> healthyNodes = nodeManager.getHealthyNodes();

        if (healthyNodes.isEmpty()) {
            LOGGER.warning("No healthy nodes available for file retrieval");
            return null;
        }

        // Try each healthy node until file is found
        for (Node node : healthyNodes) {
            byte[] fileData = retrieveFileFromNode(node, filePath);
            if (fileData != null) {
                return fileData;
            }
        }

        LOGGER.warning("File not found on any healthy node: " + filePath);
        return null;
    }

    /**
     * Retrieve file from a specific node
     */
    public byte[] retrieveFileFromNode(Node node, String filePath) {
        try {
            // URL encode the file path to handle spaces and special characters
            String encodedFilePath = java.net.URLEncoder.encode(filePath, "UTF-8");
            String url = String.format("http://%s:%d/node/files?filePath=%s",
                node.getAddress(), node.getPort(), encodedFilePath);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                LOGGER.info("File retrieved successfully from node " + node.getNodeId() + ": " + filePath);
                return response.body();
            } else if (response.statusCode() == 404) {
                LOGGER.info("File not found on node " + node.getNodeId() + ": " + filePath);
                return null;
            } else {
                LOGGER.warning("Failed to retrieve file from node " + node.getNodeId() + ": " + response.statusCode());
                return null;
            }

        } catch (Exception e) {
            LOGGER.severe("Error retrieving file from node " + node.getNodeId() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Delete file from all nodes
     */
    public boolean deleteFile(String filePath) {
        List<Node> healthyNodes = nodeManager.getHealthyNodes();

        if (healthyNodes.isEmpty()) {
            LOGGER.warning("No healthy nodes available for file deletion");
            return false;
        }

        boolean anyDeleted = false;

        // Try to delete from all healthy nodes
        for (Node node : healthyNodes) {
            if (deleteFileFromNode(node, filePath)) {
                anyDeleted = true;
            }
        }

        return anyDeleted;
    }

    /**
     * Delete file from a specific node
     */
    public boolean deleteFileFromNode(Node node, String filePath) {
        try {
            // URL encode the file path to handle spaces and special characters
            String encodedFilePath = java.net.URLEncoder.encode(filePath, "UTF-8");
            String url = String.format("http://%s:%d/node/files?filePath=%s",
                node.getAddress(), node.getPort(), encodedFilePath);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .timeout(java.time.Duration.ofSeconds(30))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                LOGGER.info("File deleted successfully from node " + node.getNodeId() + ": " + filePath);
                return true;
            } else {
                LOGGER.warning("Failed to delete file from node " + node.getNodeId() + ": " + response.statusCode());
                return false;
            }

        } catch (Exception e) {
            LOGGER.severe("Error deleting file from node " + node.getNodeId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Perform health check on a node using simple HTTP
     */
    public boolean performHealthCheck(Node node) {
        try {
            String url = String.format("http://%s:%d/node/health", node.getAddress(), node.getPort());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(5))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            boolean isHealthy = response.statusCode() == 200;

            if (isHealthy) {
                nodeManager.markNodeHealthy(node.getNodeId());
                LOGGER.info("Health check passed for node: " + node.getNodeId());
            } else {
                nodeManager.markNodeUnhealthy(node.getNodeId());
                LOGGER.warning("Health check failed for node: " + node.getNodeId());
            }

            return isHealthy;

        } catch (Exception e) {
            nodeManager.markNodeUnhealthy(node.getNodeId());
            LOGGER.severe("Health check error for node " + node.getNodeId() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get node info using simple HTTP
     */
    public String getNodeInfo(Node node) {
        try {
            String url = String.format("http://%s:%d/node/info", node.getAddress(), node.getPort());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(java.time.Duration.ofSeconds(10))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                return null;
            }

        } catch (Exception e) {
            LOGGER.severe("Error getting node info from " + node.getNodeId() + ": " + e.getMessage());
            return null;
        }
    }
}
