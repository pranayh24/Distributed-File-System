package org.pr.dfs.replication;

import lombok.Data;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class NodeStatus {
    private long lastHeartbeat;
    private boolean healthy;
    private Set<String> hostedFiles;

    public NodeStatus() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.healthy = true;
        this.hostedFiles = ConcurrentHashMap.newKeySet();
    }

    public boolean isHealthy() {
        return healthy && (System.currentTimeMillis() - lastHeartbeat) < 45000;
    }

    public void markUnhealthy() {
        this.healthy = false;
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.healthy = true;
    }

}
