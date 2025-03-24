package org.pr.dfs.replication;


import org.pr.dfs.model.FileOperationResult;
import org.pr.dfs.model.Node;
import org.pr.dfs.model.ReplicationStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReplicationManager {
    private static final Logger LOGGER = Logger.getLogger(ReplicationManager.class.getName());

    private static final int DEFAULT_REPLICATION_FACTOR = 3;
    private static final int REPLICATION_TIMEOUT_MS = 30000;
    private static final int MAX_REPLICATION_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private final ExecutorService replicationExecutor;

    private final ConcurrentHashMap<String, ReplicationStatus> fileReplicationStatus;
    private final ConcurrentHashMap<String, Set<String>> nodeToFilesMap; // Maps files to their replica nodes
    private final ConcurrentHashMap<String, Object> fileLocks;
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingReplications;

    private final int defaultReplicationFactor;
    private final NodeManager nodeManager;

    public ReplicationManager(int replicationFactor, NodeManager nodeManager) {
        this.defaultReplicationFactor = Math.max(1, replicationFactor);
        this.nodeManager = nodeManager;
        this.fileReplicationStatus = new ConcurrentHashMap<>();
        this.nodeToFilesMap = new ConcurrentHashMap<>();
        this.fileLocks = new ConcurrentHashMap<>();
        this.pendingReplications = new ConcurrentHashMap<>();
        this.replicationExecutor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

        LOGGER.info("Replication initialized with default factor: " + defaultReplicationFactor);
    }


    public CompletableFuture<Boolean> replicateFile(String filePath) {
        return replicateFile(filePath, defaultReplicationFactor);
    }


    public CompletableFuture<Boolean> replicateFile(String filePath, int targetReplicationFactor) {
        // Check if replication is already in progress
        CompletableFuture<Boolean> pendingFuture = pendingReplications.get(filePath);
        if(pendingFuture != null && !pendingFuture.isDone()) {
            LOGGER.info("Replication already in progress for " + filePath);
            return pendingFuture;
        }

        // Create a new future for this replication task
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        pendingReplications.put(filePath, future);

        // Get lock for this file
        Object fileLock = fileLocks.computeIfAbsent(filePath, k -> new Object());

        // Submit replication task
        replicationExecutor.submit(() -> {
            try {
                synchronized (fileLock) {
                    boolean success = doReplicateFile(filePath, targetReplicationFactor);
                    future.complete(success);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Replication failed for " + filePath, e);
                future.completeExceptionally(e);
            } finally {
                pendingReplications.remove(filePath);
            }
        });

        return future;
    }

    private boolean doReplicateFile(String filePath, int targetReplicationFactor) {
        try {
            // Get current replication status
            ReplicationStatus status = fileReplicationStatus.get(filePath);
            int currentReplicas = status.getCurrentReplicas();
            int neededReplicas = Math.max(0, targetReplicationFactor - currentReplicas);

            if(neededReplicas <= 0) {
                LOGGER.info("File " + filePath + " already has sufficient replicas");
                return true;
            }

            LOGGER.info("Replicating " + filePath + ": need " + neededReplicas + " more replicas to achieve factor " + targetReplicationFactor);

            // Get available nodes for replication
            List<Node> availableNodes = new ArrayList<>(nodeManager.getHealthyNodes());
            availableNodes.removeIf(node -> status.getNodeIds().contains(node.getNodeId()));

            if(availableNodes.isEmpty()) {
                LOGGER.info("No available nodes for replication of " + filePath);
                return false;
            }

            // Read files data once
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            int successCount = 0;

            // Try to replicate to as many as needed
            for(int i = 0;i< Math.min(neededReplicas, availableNodes.size()); i++) {
                Node targetNode = availableNodes.get(i);
                if(replicateToNodeWithRetry(filePath, fileData, targetNode)) {
                    successCount++;

                    // Add node to replication status
                    status.addNode(targetNode);

                    // Update node-to-files mapping
                    addFileToNodeMapping(filePath, targetNode.getNodeId());

                    LOGGER.info("Successfully replicated " + filePath + " to " + targetNode.getNodeId());
                }
            }

            // Update replication status
            fileReplicationStatus.put(filePath, status);

            LOGGER.info("Replication completed for " + filePath + ": " + successCount + "/" + neededReplicas + " successful");

            return successCount >= neededReplicas;

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error replicating file" + filePath, e);
            return false;
        }
    }

    private boolean replicateToNodeWithRetry(String filePath, byte[] fileData, Node targetNode) {
        for(int attempt = 0; attempt < MAX_REPLICATION_RETRIES; attempt++) {
            try {
                LOGGER.info("Replicating " + filePath + " to node " + targetNode.getNodeId() + " (attempt " + (attempt + 1) + ")");

                boolean success = targetNode.transferFile(filePath, fileData);

                if(success) {
                    return true;
                }

                LOGGER.warning("Replication attempt " + (attempt + 1) + " failed for " + filePath);

                // wait before retry
                if(attempt < MAX_REPLICATION_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in replication attempt " + (attempt + 1 ) + " for " + filePath + " to node " + targetNode.getNodeId(), e);

                // Check of node is still healthy
                if(!nodeManager.isNodeHealthy(targetNode.getNodeId())) {
                    LOGGER.warning("Node " + targetNode.getNodeId() + " is unhealthy, aborting replication");
                    return false;
                }

                // Wait before retry
                if(attempt < MAX_REPLICATION_RETRIES - 1) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                    } catch(InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        return false;
    }

    public boolean handleFileDeletion(String filePath) {
        ReplicationStatus status = fileReplicationStatus.get(filePath);
        if(status != null || status.getNodeIds().isEmpty()) {
            LOGGER.warning("No replication information found for file " + filePath);
            fileReplicationStatus.remove(filePath);
            return true;
        }

        boolean allSuccessful = true;

        for(String nodeId : status.getNodeIds()) {
            try {
                Node node = nodeManager.getNodeById(nodeId);
                if(node == null) {
                    LOGGER.warning("Node " + nodeId + " not found for file deletion");
                    continue;
                }

                if(node.deleteFile(filePath)) {
                    LOGGER.info("Successfully deleted file " + filePath + " from node " + nodeId);
                    removeFileFromNodeMapping(filePath, nodeId);
                } else {
                    LOGGER.warning("Failed to delete file " + filePath + " from node " + nodeId);
                    allSuccessful = false;
                }
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Error deleting file " + filePath + " from node " + nodeId, e);
                allSuccessful = false;
            }
        }

        // Clean up replication status
        fileReplicationStatus.remove(filePath);

        return allSuccessful;
    }

    public ReplicationStatus getReplicationStatus(String filePath) {
        return fileReplicationStatus.computeIfAbsent(filePath, path -> {
            ReplicationStatus status = new ReplicationStatus(path, defaultReplicationFactor);

            for(Node node : nodeManager.getHealthyNodes()) {
                if(node.hasFile(filePath)) {
                    status.addNode(node);
                    addFileToNodeMapping(filePath, node.getNodeId());
                }
            }
            return status;
        });
    }

    private void addFileToNodeMapping(String filePath, String nodeId) {
        nodeToFilesMap.computeIfAbsent(nodeId, k -> ConcurrentHashMap.newKeySet()).add(filePath);
    }

    private void removeFileFromNodeMapping(String filePath, String nodeId) {
        Set<String> files = nodeToFilesMap.get(nodeId);
        if(files != null) {
            files.remove(filePath);
        }
    }

    public Set<String> getFilesOnNode(String nodeId) {
        return nodeToFilesMap.getOrDefault(nodeId, Collections.emptySet());
    }

    public CompletableFuture<FileOperationResult> recoverFromNodeFailure(String nodeId) {
        CompletableFuture<FileOperationResult> future = new CompletableFuture<>();

        replicationExecutor.submit(() -> {
            try {
                Set<String> filesToRecover = new HashSet<>(getFilesOnNode(nodeId));
                LOGGER.info("Starting recovery for node " + nodeId + " with " + filesToRecover.size() + " files");

                // Create a list to track individual file recovery results
                List<CompletableFuture<Boolean>> recoveryTasks = new ArrayList<>();

                for(String filePath : filesToRecover) {

                    CompletableFuture<Boolean> task = replicateFile(filePath);
                    recoveryTasks.add(task);
                }

                // Wait for all recovery tasks to complete
                CompletableFuture<Void> allTasks= CompletableFuture.allOf(recoveryTasks.toArray(new CompletableFuture[0]));

                try {
                    allTasks.get(5, TimeUnit.SECONDS);
                } catch(TimeoutException te) {
                    LOGGER.warning("Recovery operation timed out for " + nodeId);
                }

                // Count successful recoveries
                long successCount = recoveryTasks.stream()
                        .filter(task -> {
                            try {
                                return task.isDone() && !task.isCompletedExceptionally() && task.get();
                            } catch(Exception e) {
                                return false;
                            }
                        })
                        .count();

                FileOperationResult result = new FileOperationResult(successCount > 0, "Recovered " + successCount + "/ " + filesToRecover.size() + " files from failed node " + nodeId);

                LOGGER.info(result.getMessage());
                future.complete(result);
            } catch(Exception e) {
                LOGGER.log(Level.SEVERE, "Error recovering from node " + nodeId, e);

                FileOperationResult result = new FileOperationResult(false, "Recovery failed for node " + nodeId);
                future.complete(result);
            }
        });

        return future;
    }

    public void setReplicationFactor(String filePath, int replicationFactor) {
        ReplicationStatus status = fileReplicationStatus.get(filePath);
        status.setReplicationFactor(Math.max(1,replicationFactor));
        fileReplicationStatus.put(filePath, status);
    }

    public void checkAndReplicateFiles() {
        Map<String, ReplicationStatus> statusMap = new HashMap<>(fileReplicationStatus);

        for(Map.Entry<String, ReplicationStatus> entry : statusMap.entrySet()) {
            String filePath = entry.getKey();
            ReplicationStatus status = entry.getValue();

            if(status.getCurrentReplicas() < status.getReplicationFactor()) {
                LOGGER.info("Auto-replicating " + filePath + " to meet factor " + status.getReplicationFactor());
                replicateFile(filePath, status.getReplicationFactor());
            }
        }
    }

    public void shutdown() {
        replicationExecutor.shutdown();
        try {
            if(!replicationExecutor.awaitTermination(30, TimeUnit.SECONDS));
            replicationExecutor.shutdownNow();
        } catch(InterruptedException ie) {
            replicationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}