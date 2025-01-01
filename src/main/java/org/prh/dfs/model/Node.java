package org.prh.dfs.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class Node implements Serializable {
    private final String nodeId;
    private final String ipAddress;
    private final int port;
    private boolean isAlive;
    private long lastHeartbeat;

    public Node(String nodeId, String ipAddress, int port) {
        this.nodeId = nodeId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isAlive = true;
        this.lastHeartbeat = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeId='" + nodeId + '\'' +
                ", port=" + port +
                ", isAlive=" + isAlive +
                '}';
    }

    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.isAlive = true;
    }
}
