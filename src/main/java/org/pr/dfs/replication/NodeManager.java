package org.pr.dfs.replication;

import org.pr.dfs.model.Node;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NodeManager {
    private static final Logger LOGGER = Logger.getLogger(NodeManager.class.getName());
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int HEALTH_CHECK_PORT = 8889;

    private final Map<String, Node> nodes;
    private final List<NodeStatusListener> listeners;

    public interface NodeStatusListener {
        void onNodeFailure(String nodeId);
    }

    public NodeManager() {
        this.nodes = new ConcurrentHashMap<>();
        this.listeners = Collections.synchronizedList(new ArrayList<>());
        LOGGER.info("NodeManager initialized");
    }

    public boolean registerNode(Node node) {
        if(node == null || node.getNodeId() == null) {
            return false;
        }

        nodes.put(node.getNodeId(), node);
        LOGGER.info("Node registered " + node.getNodeId());
        return true;
    }

    public boolean unRegisterNode(String nodeId) {
        Node removed = nodes.remove(nodeId);
        if(removed != null) {
            LOGGER.info("Node unregistered " + nodeId);
            return true;
        }
        return false;
    }

    public List<Node> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    public List<Node> getHealthyNodes() {
        List<Node> healthyNodes = new ArrayList<>();
        for(Node node : nodes.values()) {
            if(node.isHealthy()) {
                healthyNodes.add(node);
            }
        }
        return healthyNodes;
    }

    public Node getNodeById(String nodeId) {
        return nodes.get(nodeId);
    }

    public void addNodeStatusListener(NodeStatusListener listener) {
        if(listener != null) {
            listeners.add(listener);
        }
    }

    public void removeNodeStatusListener(NodeStatusListener listener) {
        listeners.remove(listener);
    }

    public boolean isNodeHealthy(String nodeId) {
        Node node = nodes.get(nodeId);
        return node != null && node.isHealthy();
    }

    public void markNodeHealthy(String nodeId) {
        Node node = nodes.get(nodeId);
        if(node != null) {
            node.setHealthy(true);
            node.setLastHeartbeat(System.currentTimeMillis());
            LOGGER.info("Node marked healthy " + nodeId);
        }
    }

    public void markNodeUnhealthy(String nodeId) {
        Node node = nodes.get(nodeId);
        if(node != null) {
            boolean wasHealthy = node.isHealthy();
            node.setHealthy(false);

            if(wasHealthy) {
                LOGGER.info("Node marked unhealthy " + nodeId);
                notifyNodeFailure(nodeId);
            }
        }
    }

    public void checkNodeHealth(Node node) {
        if(node == null) return;

        boolean previousHealth = node.isHealthy();
        boolean currentHealth = isNodeResponsive(node);

        if(currentHealth != previousHealth) {
            node.setHealthy(currentHealth);

            if(!currentHealth && previousHealth) {
                LOGGER.warning("Node " + node.getNodeId() + " is now unhealthy");
                notifyNodeFailure(node.getNodeId());
            } else if(currentHealth && !previousHealth) {
                LOGGER.warning("Node " + node.getNodeId() + " is now healthy");
                node.setLastHeartbeat(System.currentTimeMillis());
            }
        }

        if(currentHealth) {
            node.setLastHeartbeat(System.currentTimeMillis());
        }
    }

    private boolean isNodeResponsive(Node node) {
        try {
            // Make HTTP health check to actual node server
            String healthUrl = "http://" + node.getAddress() + ":" + node.getPort() + "/node/health";

            // Simple HTTP connection test
            java.net.URL url = new java.net.URL(healthUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECTION_TIMEOUT_MS);
            connection.setReadTimeout(CONNECTION_TIMEOUT_MS);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            boolean isHealthy = responseCode == 200;
            if (isHealthy) {
                LOGGER.info("Node " + node.getNodeId() + " health check passed");
            }

            return isHealthy;

        } catch(Exception e) {
            LOGGER.fine("Node " + node.getNodeId() + " health check failed: " + e.getMessage());
            return false;
        }
    }

    private void notifyNodeFailure(String nodeId) {
        for (NodeStatusListener listener : listeners) {
            try {
                listener.onNodeFailure(nodeId);
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Error notifying listener about node failure");
            }
        }
    }

    public void notifyNodeShutdown(String nodeId) {
        // Remove the node
        unRegisterNode(nodeId);

        // Notify listeners
        notifyNodeFailure(nodeId);
    }

    public void updateNodeInfo(String nodeId, long availableDiskSpace, int hostedFilesCount) {
        Node node = nodes.get(nodeId);
        if(node != null) {
            node.setAvailableDiskSpace(availableDiskSpace);
            LOGGER.info("Node " + nodeId + " has " + hostedFilesCount + " hosted files");
            node.setLastHeartbeat(System.currentTimeMillis());
        }
    }

}
