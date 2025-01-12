package org.pr.dfs.replication;


import org.pr.dfs.model.Node;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ReplicationManager {
    private static final Logger LOGGER = Logger.getLogger(ReplicationManager.class.getName());
    private final ConcurrentHashMap<String, List<Node>> fileToNodesMap; // Maps files to their replica nodes
    private final Map<String, byte[]> fileDataCache; // Temporary cache for file data
    private final int replicationFactor;
    private final NodeManager nodeManager;

    public ReplicationManager(int replicationFactor, NodeManager nodeManager) {
        this.replicationFactor = replicationFactor;
        this.fileToNodesMap = new ConcurrentHashMap<>();
        this.fileDataCache = new ConcurrentHashMap<>();
        this.nodeManager = nodeManager;
    }


    public void replicateFile(String filePath) {
        try {
            List<Node> currentNodes = fileToNodesMap.getOrDefault(filePath, new CopyOnWriteArrayList<>());
            int neededReplicas = replicationFactor - currentNodes.size();

            if (neededReplicas <= 0) return;

            List<Node> availableForReplication = new ArrayList<>(nodeManager.getHealthyNodes());
            availableForReplication.removeAll(currentNodes);

            for (int i = 0; i < Math.min(neededReplicas, availableForReplication.size()); i++) {
                Node targetNode = availableForReplication.get(i);
                replicateToNode(filePath, targetNode);
            }
        } catch(Exception e) {

        }
    }

    private void replicateToNode(String filePath, Node targetNode) {
        try {
            byte[] data = Files.readAllBytes(Path.of(filePath));
            targetNode.transferFile(filePath, data);
            List<Node> nodes = fileToNodesMap.getOrDefault(filePath, new CopyOnWriteArrayList<>());
            nodes.add(targetNode);
            fileToNodesMap.putIfAbsent(filePath, nodes);
            LOGGER.info("Successfully replicated " + filePath + " to node " + targetNode);
        } catch(Exception e) {
            LOGGER.warning("Failed to replicate: " + filePath + " to " + targetNode + ": " + e.getMessage());

        }
    }
}
