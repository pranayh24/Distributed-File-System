package org.pr.dfs.service;

import org.pr.dfs.model.FilePlacement;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Single source of truth for file → node placement across the cluster.
 * Populated by the upload path and mutated by self-healing; read by the
 * system API so the dashboard can show exactly which nodes hold each file.
 */
@Service
public class PlacementRegistry {

    private final Map<String, FilePlacement> byPath = new ConcurrentHashMap<>();

    public FilePlacement record(String filePath, String fileName, String userId,
                                long size, int replicationFactor, Collection<String> nodeIds) {
        FilePlacement placement = new FilePlacement(filePath, fileName, userId, size, replicationFactor);
        nodeIds.forEach(placement::addNode);
        byPath.put(filePath, placement);
        return placement;
    }

    public void addReplica(String filePath, String nodeId) {
        FilePlacement placement = byPath.get(filePath);
        if (placement != null) {
            placement.addNode(nodeId);
        }
    }

    public void removeReplica(String filePath, String nodeId) {
        FilePlacement placement = byPath.get(filePath);
        if (placement != null) {
            placement.removeNode(nodeId);
        }
    }

    public void remove(String filePath) {
        byPath.remove(filePath);
    }

    public FilePlacement get(String filePath) {
        return byPath.get(filePath);
    }

    public Collection<FilePlacement> all() {
        return byPath.values();
    }

    /** Every file that currently has a replica recorded on the given node. */
    public List<FilePlacement> filesWithReplicaOn(String nodeId) {
        return byPath.values().stream()
                .filter(p -> p.getNodeIds().contains(nodeId))
                .collect(Collectors.toList());
    }
}
