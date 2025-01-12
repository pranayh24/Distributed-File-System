package org.pr.dfs.replication;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class FaultToleranceManager implements NodeManager.NodeStatusListener {
     private static final Logger LOGGER = Logger.getLogger(FaultToleranceManager.class.getName());
     private final ReplicationManager replicationManager;
     private final ConcurrentHashMap<String, NodeStatus> nodeStatuses;

     public FaultToleranceManager(NodeManager nodeManager, ReplicationManager replicationManager) {
         this.replicationManager = replicationManager;
         this.nodeStatuses = new ConcurrentHashMap<>();
         nodeManager.addNodeStatusListener(this);
     }

     @Override
     public void onNodeFailure(String nodeId) {
         LOGGER.warning("Node failure detected: " + nodeId);
         handleNodeFailure(nodeId);
     }

    private void checkNodesHealth() {
         nodeStatuses.forEach((nodeId, status) -> {
             if(!status.isHealthy()) {
                 handleNodeFailure(nodeId);
             }
         });
    }

    private void handleNodeFailure(String nodeId) {
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


