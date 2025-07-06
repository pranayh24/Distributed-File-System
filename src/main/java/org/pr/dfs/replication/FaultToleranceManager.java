package org.pr.dfs.replication;

import org.pr.dfs.model.FileOperationResult;
import org.pr.dfs.model.Node;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FaultToleranceManager implements NodeManager.NodeStatusListener {
    private static final Logger LOGGER = Logger.getLogger(FaultToleranceManager.class.getName());

    private static final long HEARTBEAT_TIMEOUT_MS = 60000; // 60 seconds
    private static final long NODE_RECOVERY_COOLDOWN_MS = 300000; // 5 minutes

    private final NodeManager nodeManager;
    private final ReplicationManager replicationManager;
    private final ConcurrentHashMap<String, NodeStatus> nodeStatuses;
    private final ConcurrentHashMap<String, Long> nodeRecoveryTimes;
    private final ScheduledExecutorService recoveryExecutor;

    public FaultToleranceManager(NodeManager nodeManager, ReplicationManager replicationManager) {
        this.nodeManager = nodeManager;
        this.replicationManager = replicationManager;
        this.nodeStatuses = new ConcurrentHashMap<>();
        this.nodeRecoveryTimes = new ConcurrentHashMap<>();
        this.recoveryExecutor = Executors.newScheduledThreadPool(2);

        nodeManager.addNodeStatusListener(this);
        LOGGER.info("Fault Tolerance Manager initialized");
    }

    @Override
    public void onNodeFailure(String nodeId) {
        LOGGER.warning("Node failure detected: " + nodeId);
        handleNodeFailure(nodeId);
    }

    private void checkNodesHealth() {
        LOGGER.info("Checking health of all nodes");

        for(Map.Entry<String, NodeStatus> entry : nodeStatuses.entrySet()) {
            String nodeId = entry.getKey();
            NodeStatus status = entry.getValue();

            if(isHeartBeatExpired(status)) {
                LOGGER.warning("Node " + nodeId + " heartbeat expired");
                handleNodeFailure(nodeId);
            }
        }
    }

    public void handleNodeFailure(String nodeId) {
        NodeStatus status = nodeStatuses.get(nodeId);

        if(status == null) {
            LOGGER.warning("No information for node: " + nodeId);
            return;
        }

        // Mark node as unhealthy
        status.markUnhealthy();
        nodeManager.markNodeUnhealthy(nodeId);  // Fixed: should mark as unhealthy, not healthy

        Long lastRecovery = nodeRecoveryTimes.get(nodeId);
        long now = System.currentTimeMillis();

        if(lastRecovery != null && (now - lastRecovery) < NODE_RECOVERY_COOLDOWN_MS) {  // Fixed: added null check
            LOGGER.info("Skipping recovery for node: " + nodeId + " - in cooldown period (" + (now - lastRecovery)/1000 + "s elapsed)");
            return;
        }

        nodeRecoveryTimes.put(nodeId, now);

        LOGGER.info("Initiating recovery for failed node " + nodeId);

        Set<String> affectedFiles = new HashSet<>(replicationManager.getFilesOnNode(nodeId));

        if(affectedFiles.isEmpty()) {
            LOGGER.info("No files to recover for node " + nodeId);
            return;
        }

        LOGGER.info("Need to recover " + affectedFiles.size() + " files from node " + nodeId);

        recoveryExecutor.submit(() -> {
            try {
                replicationManager.recoverFromNodeFailure(nodeId)
                        .thenAccept(result -> {
                            if(result.isSuccess()) {
                                LOGGER.info("Recovery completed for node " + nodeId + ": " + result.getMessage());
                            } else {
                                LOGGER.warning("Recovery failed for node " + nodeId + ": " + result.getMessage());
                            }
                        });
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during recovery for node " + nodeId, e);
            }
        });
    }

    public void registerHeartbeat(String nodeId) {
        nodeStatuses.computeIfAbsent(nodeId, k -> new NodeStatus())
                .updateHeartbeat();

        // Mark node as healthy if it wasn't
        Node node = nodeManager.getNodeById(nodeId);
        if(node != null && !node.isHealthy()) {
            LOGGER.info("Node " + nodeId + " is back online");
            nodeManager.markNodeHealthy(nodeId);
        }
    }

    public void registerHostedFiles(String nodeId, Set<String> files) {
        NodeStatus status = nodeStatuses.computeIfAbsent(nodeId, k -> new NodeStatus());
        status.setHostedFiles(files);
    }

    public void checkAndRecover() {
        LOGGER.info("Running recovery check");

        // First check node health
        List<Node> allNodes = nodeManager.getAllNodes();
        for(Node node : allNodes) {
            if(!node.isHealthy() && !isNodeInRecovery(node.getNodeId())) {
                LOGGER.info("Detected unhealthy node during recovery check: " + node.getNodeId());
                handleNodeFailure(node.getNodeId());
            }
        }

        // Then check file replication status
        replicationManager.checkAndReplicateFiles();
    }

    private boolean isNodeInRecovery(String nodeId) {
        Long lastRecovery = nodeRecoveryTimes.get(nodeId);
        if(lastRecovery == null) {
            return false;
        }

        return System.currentTimeMillis() - lastRecovery < NODE_RECOVERY_COOLDOWN_MS;
    }


    private boolean isHeartBeatExpired(NodeStatus status) {
        return status.getLastHeartbeat() + HEARTBEAT_TIMEOUT_MS < System.currentTimeMillis();
    }

    public CompletableFuture<FileOperationResult> initiateRecovery(String nodeId) {
        if(nodeId == null || nodeId.trim().isEmpty()) {

            List<Node> allNodes = nodeManager.getAllNodes();
            List<CompletableFuture<FileOperationResult>> recoveryTasks = new ArrayList<>();

            for(Node node : allNodes) {
                if(!node.isHealthy()) {
                    LOGGER.info("Detected unhealthy node during recovery: " + node.getNodeId());
                    recoveryTasks.add(replicationManager.recoverFromNodeFailure(node.getNodeId()));
                }
            }

            if(recoveryTasks.isEmpty()) {
                LOGGER.info("No unhealthy nodes found for recovery");
                CompletableFuture<FileOperationResult> result = new CompletableFuture<>();
                FileOperationResult noActionResult = new FileOperationResult(true, "No unhealthy nodes found that require recovery");
                result.complete(noActionResult);
                return result;
            }

            // Combine all recovery tasks
            return CompletableFuture.allOf(recoveryTasks.toArray(new CompletableFuture[0]))
                    .thenApply(v -> {
                        FileOperationResult combinedResult = new FileOperationResult(true, "Recovery initiated for " + recoveryTasks.size() + " nodes");
                        return combinedResult;
                    });
        } else {
            // Recover specific node
            Node node = nodeManager.getNodeById(nodeId);
            if (node == null) {
                CompletableFuture<FileOperationResult> result = new CompletableFuture<>();
                FileOperationResult errorResult = new FileOperationResult(false, "Node " + nodeId + " not found");
                result.complete(errorResult);
                return result;
            }

            LOGGER.info("Manually initiating recovery for node: " + nodeId);
            nodeRecoveryTimes.put(nodeId, System.currentTimeMillis());
            return replicationManager.recoverFromNodeFailure(node.getNodeId());
        }
    }

    public void shutdown() {
        recoveryExecutor.shutdown();
        try {
            if(!recoveryExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                recoveryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            recoveryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
