package org.pr.dfs.replication;

import lombok.Getter;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class NodeStatus {
    private long lastHeartbeat;
    private boolean healthy;
    @Getter
    private final Set<String> hostedFiles;

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
