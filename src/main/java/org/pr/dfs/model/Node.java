package org.pr.dfs.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Node implements Serializable {
    private final String address;
    private final int port;
    private boolean healthy;
    private long lastHeartbeat;

    public Node(String address, int port) {
        this.address = address;
        this.port = port;
        this.healthy = true;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public boolean isHealthy() {
        return healthy && System.currentTimeMillis() - lastHeartbeat > 60000; // 60 second timeout
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
    }

    public void transferFile(String filePath) {
    }
}
