package org.pr.dfs.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ReplicationStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String filePath;
    private final Set<String> nodeIds;
    private String primaryNodeId;
    private int replicationFactor;
    private long lastUpdated;

    public ReplicationStatus(String filePath, int replicationFactor) {
        this.filePath = filePath;
        this.nodeIds = ConcurrentHashMap.newKeySet();
        this.replicationFactor = replicationFactor;
        this.lastUpdated = System.currentTimeMillis();
    }

    public void addNode(Node node) {
        if(node == null) return;

        nodeIds.add(node.getNodeId());

        // Set as primary node if none exists
        if(primaryNodeId == null) {
            primaryNodeId = node.getNodeId();
        }

        lastUpdated = System.currentTimeMillis();
    }

    private boolean removeNode(String nodeId) {
        boolean removed = nodeIds.remove(nodeId);

        if(removed && nodeId.equals(primaryNodeId) && !nodeIds.isEmpty()) {
            primaryNodeId = nodeIds.iterator().next();
        }

        if(removed) {
            lastUpdated = System.currentTimeMillis();
        }

        return removed;
    }

    public Set<String> getNodeIds() {
        return Collections.unmodifiableSet(nodeIds);
    }

    public String getFilePath() {
        return filePath;
    }

    public int getCurrentReplicas() {
        return nodeIds.size();
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
        lastUpdated = System.currentTimeMillis();
    }

    public String getPrimaryNodeId() {
        return primaryNodeId;
    }

    public void setPrimaryNodeId(String primaryNodeId) {
        this.primaryNodeId = primaryNodeId;
        lastUpdated = System.currentTimeMillis();
    }

    public long getLastUpdated() {
        return lastUpdated;
    }
}
