package org.pr.dfs.fault;

import org.pr.dfs.model.Node;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class HeartbeatMonitor {
    private static final Logger LOGGER = Logger.getLogger(HeartbeatMonitor.class.getName());
    private static final long HEARTBEAT_INTERVAL = 5000; // 5 secs
    private static final long HEARTBEAT_TIMEOUT = 15000; // 15 secs

    private final Map<String, Node> nodes;
    private final ScheduledExecutorService scheduler;
    private final NodeFailureHandler failureHandler;

    public HeartbeatMonitor(NodeFailureHandler failureHandler) {
        this.nodes = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.failureHandler = failureHandler;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::checkHeartbeats,
                HEARTBEAT_INTERVAL,
                HEARTBEAT_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        LOGGER.info("Heartbeat monitoring started");
    }

    public void registerNode(Node node) {
        nodes.put(node.getNodeId(), node);
        LOGGER.info("Registered node: " + node.getNodeId());
    }

    public void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();

        nodes.values().forEach(node -> {
           if(node.isAlive() && (currentTime - node.getLastHeartbeat() > HEARTBEAT_TIMEOUT)) {
               handleNodeFailure(node);
           }
        });
    }

    private void handleNodeFailure(Node node) {
        node.setAlive(false);
        LOGGER.warning("Node failure detected: " + node.getNodeId());
        failureHandler.handleNodeFailure(node);
    }

    public void stop() {
        scheduler.shutdown();
        try {
            if(!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdown();
            }
        } catch(InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void recieveHeartbeat(String nodeId) {
        Node node = nodes.get(nodeId);
        if (node != null) {
            node.updateHeartbeat();
            LOGGER.info("Heartbeat received from node: " + nodeId);
        } else {
            LOGGER.warning("Received heartbeat from unknown node: " + nodeId);
        }
    }
}
