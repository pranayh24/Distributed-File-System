package org.pr.dfs.integration;

import lombok.Getter;
import org.pr.dfs.fault.FailureRecoveryManager;
import org.pr.dfs.fault.HeartbeatMonitor;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.ReplicationManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@Getter
public class DFSIntegrator {
    private static final Logger LOGGER = Logger.getLogger(DFSIntegrator.class.getName());

    private final ReplicationManager replicationManager;
    private final HeartbeatMonitor heartbeatMonitor;
    private final FailureRecoveryManager failureRecoveryManager;
    private final Map<String, Node> activeNodes;

    public DFSIntegrator() {
        this.activeNodes = new ConcurrentHashMap<>();
        this.replicationManager = new ReplicationManager();
        this.failureRecoveryManager = new FailureRecoveryManager(replicationManager);
        this.heartbeatMonitor = new HeartbeatMonitor(failureRecoveryManager);
    }

    public void initializeSystem() {
        // Start heartbeat monitoring
        heartbeatMonitor.start();
        LOGGER.info("DFS Integration layer initialized");
    }

    public String addNode(String ipAddress, int port) {
        // Create and register a new node
        String nodeId = generateNodeId(ipAddress, port);
        Node newNode = new Node(ipAddress, port);

        // Register with all components
        activeNodes.put(nodeId, newNode);
        replicationManager.addNode(newNode);
        heartbeatMonitor.registerNode(newNode);

        LOGGER.info("Node registered across all components: " + nodeId);
        return nodeId;
    }

    public CompletableFuture<Boolean> writeFile(String fileName, byte[] data) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Check if we have enough active nodes for replication
                if (activeNodes.size() < ReplicationManager.REPLICATION_FACTOR) {
                    LOGGER.warning("Not enough active nodes for replication");
                    return false;
                }

                // Trigger Replication
                boolean replicationSuccess = replicationManager.replicateFile(fileName, data)
                        .get(30, TimeUnit.SECONDS);
                if(replicationSuccess) {
                    // Track file for failure recovery
                    trackFileForRecovery(fileName);
                    LOGGER.info("File successfully written and tracked: " + fileName);
                    return  true;
                } else {
                    LOGGER.severe("Failed to replicate file: " + fileName);
                    return false;
                }
            } catch(Exception e) {
                LOGGER.severe("Error in write operation: " + e.getMessage());
                return false;
            }
        });
    }

    private void trackFileForRecovery(String fileName) {
        // Get nodes containing the file
        Set<Node> replicaNodes = replicationManager.getFileReplicas(fileName);

        // Register with failure recovery manager
        for (Node node : replicaNodes) {
            failureRecoveryManager.trackFile(node.getNodeId(), fileName);
        }
    }

    void handleNodeFailure(String nodeId) {
        Node failedNode = activeNodes.get(nodeId);
        if (failedNode != null) {
            // Update node status
            failedNode.setAlive(false);

            // Trigger failure recovery
            failureRecoveryManager.handleNodeFailure(failedNode);

            // Remove from active nodes
            activeNodes.remove(nodeId);

            LOGGER.warning("Node failure handled: " +nodeId);
        }
    }

    public void handleHeartbeat(String nodeId) {
        Node node = activeNodes.get(nodeId);
        if(node != null ) {
            heartbeatMonitor.recieveHeartbeat(nodeId);
            node.updateHeartbeat();

            // If node was previously marked as failed, restore it
            if(!node.isAlive()) {
                node.setAlive(true);
                replicationManager.addNode(node);
                LOGGER.info("Node restored after heartbeat: " + nodeId);
            }
        }
    }

    private String generateNodeId(String ipAddress, int port) {
        return String.format("node_%s_%d_%d",
                ipAddress.replace('.','_'),
                port,System.currentTimeMillis() % 10000);
    }

    public void shutdown() {
        try {
            heartbeatMonitor.stop();

            LOGGER.info("DFS Integration layer shutdown complete");
        } catch(Exception e) {
            LOGGER.severe("Error during shutdown: " + e.getMessage());
        }
    }
}
