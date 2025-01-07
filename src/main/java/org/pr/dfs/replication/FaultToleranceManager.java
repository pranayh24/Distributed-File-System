package org.pr.dfs.replication;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class FaultToleranceManager {
     private static final Logger LOGGER = Logger.getLogger(FaultToleranceManager.class.getName());
     private final ReplicationManager replicationManager;
     private final ConcurrentHashMap<String, NodeStatus> nodeStatuses;
     private final ScheduledExecutorService healthChecker;

     public FaultToleranceManager(ReplicationManager replicationManager) {
         this.replicationManager = replicationManager;
         this.nodeStatuses = new ConcurrentHashMap<>();
         this.healthChecker = Executors.newScheduledThreadPool(1);
         startHealthMonitoring();
     }

    private void startHealthMonitoring() {
         healthChecker.scheduleAtFixedRate(this::checkNodesHealth, 0, 15, TimeUnit.SECONDS);
    }

    private void checkNodesHealth() {
         nodeStatuses.forEach((nodeId, status) -> {
             if(!status.isHealthy()) {
                 handleNodeFailure(nodeId);
             }
         });
    }

    private void handleNodeFailure(String nodeId) {
         LOGGER.warning("Node failure detected: " + nodeId);
         NodeStatus status = nodeStatuses.get(nodeId);
         if(status !=null) {
             status.markUnhealthy();
             // Trigger replication for affected files
             status.getHostedFiles().forEach(replicationManager::replicateFile);
         }
    }

    public void registerHeartbeat(String nodeId) {
         nodeStatuses.computeIfAbsent(nodeId, k -> new NodeStatus())
                 .updateHeartbeat();
    }
}


