package org.pr.dfs.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative record of where a single file's replicas physically live.
 * The set of node IDs is the source of truth for placement, replication level,
 * and self-healing decisions.
 */
@Getter
public class FilePlacement {

    private final String filePath;
    private final String fileName;
    private final String userId;
    private final long size;
    private volatile int replicationFactor;
    private final Set<String> nodeIds = ConcurrentHashMap.newKeySet();
    private final LocalDateTime createdAt = LocalDateTime.now();

    public FilePlacement(String filePath, String fileName, String userId, long size, int replicationFactor) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.userId = userId;
        this.size = size;
        this.replicationFactor = replicationFactor;
    }

    public void addNode(String nodeId) {
        nodeIds.add(nodeId);
    }

    public void removeNode(String nodeId) {
        nodeIds.remove(nodeId);
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }
}
