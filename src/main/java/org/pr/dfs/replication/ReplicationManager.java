package org.pr.dfs.replication;


import org.pr.dfs.model.Node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ReplicationManager {
    private static final Logger LOGGER = Logger.getLogger(ReplicationManager.class.getName());
    private final ConcurrentHashMap<String, List<Node>> fileToNodesMap; // Maps files to their replica nodes
    private final Map<String, byte[]> fileDataCache; // Temporary cache for file data
    private final List<Node> availableNodes;
    private final int replicationFactor;
    private final ScheduledExecutorService scheduler;

    public ReplicationManager(int replicationFactor) {
        this.replicationFactor = replicationFactor;
        this.fileToNodesMap = new ConcurrentHashMap<>();
        this.fileDataCache = new ConcurrentHashMap<>();
        this.availableNodes = new CopyOnWriteArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        startHealthCheck();
    }

    private void startHealthCheck() {
        scheduler.scheduleAtFixedRate(this::checkNodeHealth, 0, 30, TimeUnit.SECONDS);
    }

    private void checkNodeHealth() {
        for(Node node : availableNodes) {
            if(!node.isHealthy()) {
                handleNodeFailure(node);
            }
        }
    }

    private void handleNodeFailure(Node node) {
        availableNodes.remove(node);
        // Trigger re-replication for affected files
        for(Map.Entry<String, List<Node>> entry : fileToNodesMap.entrySet()) {
            if(entry.getValue().contains(node)) {
                replicateFile(entry.getKey());
            }
        }
    }

    public void replicateFile(String filePath) {
        List<Node> currentNodes = fileToNodesMap.getOrDefault(filePath, new CopyOnWriteArrayList<>());
        int neededReplicas = replicationFactor - currentNodes.size();

        if(neededReplicas <= 0) return;

        List<Node> availableForReplication = new ArrayList<>(availableNodes);
        availableForReplication.removeAll(currentNodes);

        for(int i =0; i < Math.min(neededReplicas, availableForReplication.size()); i++) {
            Node targetNode = availableForReplication.get(i);
            replicateToNode(filePath, targetNode);
        }
    }

    private void replicateToNode(String filePath, Node targetNode) {
        try {
            targetNode.transferFile(filePath);
            List<Node> nodes = fileToNodesMap.getOrDefault(filePath, new CopyOnWriteArrayList<>());
            nodes.add(targetNode);
        } catch(Exception e) {
            LOGGER.warning("Failed to replicate: " + filePath + " to " + targetNode + ": " + e.getMessage());

        }
    }
}
