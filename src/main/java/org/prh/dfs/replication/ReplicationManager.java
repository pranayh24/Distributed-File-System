package org.prh.dfs.replication;


import org.prh.dfs.model.Node;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

public class ReplicationManager {
    private static final Logger LOGGER = Logger.getLogger(ReplicationManager.class.getName());
    public static final int REPLICATION_FACTOR = 3;

    private final Map<String, Set<Node>> fileToNodesMap; // Maps files to their replica nodes
    private final Map<String, byte[]> fileDataCache; // Temporary cache for file data
    private final Set<Node> activeNodes;
    private final ExecutorService executorService;

    public ReplicationManager() {
        this.fileToNodesMap = new ConcurrentHashMap<>();
        this.fileDataCache = new ConcurrentHashMap<>();
        this.activeNodes = ConcurrentHashMap.newKeySet();
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void addNode(Node node) {
        activeNodes.add(node);
        LOGGER.info("Added node to replication pool: " + node.getNodeId());
    }

    public CompletableFuture<Boolean> replicateFile(String fileName, byte[] data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Store file data temporarily
                fileDataCache.put(fileName, data);

                // Select nodes for replication
                List<Node> selectedNodes = selectNodesForReplication();
                if (selectedNodes.size() < REPLICATION_FACTOR) {
                    LOGGER.warning("Not enough nodes available for replication");
                    return false;
                }

                // Create replication tasks
                List<CompletableFuture<Boolean>> replicationTasks = new ArrayList<>();
                for (Node node : selectedNodes) {
                    replicationTasks.add(replicateToNode(fileName, data, node));
                }

                // Wait for all replication to complete
                CompletableFuture<Void> allReplications = CompletableFuture.allOf(
                        replicationTasks.toArray(new CompletableFuture[0]));

                allReplications.get(30, TimeUnit.SECONDS);

                // Update file-to-nodes mapping
                fileToNodesMap.put(fileName, new HashSet<>(selectedNodes));
                return true;
            } catch(Exception e){
                LOGGER.severe("Replication failed for file: " + fileName + " Error: " + e.getMessage());
                return  false;
            }
        },executorService);
    }

    private List<Node> selectNodesForReplication() {
        List<Node> availableNodes = new ArrayList<>(activeNodes);
        if(availableNodes.size() < REPLICATION_FACTOR) {
            return availableNodes;
        }

        // Randomly select nodes for replication
        Collections.shuffle(availableNodes);
        return availableNodes.subList(0, REPLICATION_FACTOR);
    }

    private CompletableFuture<Boolean> replicateToNode(String fileName, byte[] data, Node node) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simulate network transfer and storage
                sendFileToNode(node, fileName, data);
                return true;
            } catch(Exception e) {
                LOGGER.severe("Failed to replicate to node: " + node.getNodeId());
                return false;
            }
        },executorService);
    }

    private void sendFileToNode(Node node, String fileName, byte[] data) {
        // In a real implementation, this would use network communication
        // For now, we'll simulate storage
        String storagePath = "storage/" + node.getNodeId() + "/" + fileName;
        try {
            Files.createDirectories(Path.of("storage/" + node.getNodeId()));
            Files.write(Path.of(storagePath),data);
            LOGGER.info("File " + fileName + " replicated to node " + node.getNodeId());
        } catch(Exception e) {
            throw new RuntimeException("Failed to store file on node: " + node.getNodeId());
        }
    }

    public Set<Node> getFileReplicas(String fileName) {
        return fileToNodesMap.getOrDefault(fileName, Collections.emptySet());

    }
}
