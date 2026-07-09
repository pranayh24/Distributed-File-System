package org.pr.dfs.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.FilePlacement;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.NodeManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Restores the replication factor after a node fails.
 *
 * <p>Listens for node failures (manual kill or a missed health check). For every file that
 * had a replica on the dead node, it copies the file from a surviving replica onto a fresh
 * healthy node until the target replication factor is met again — genuine self-healing,
 * using the real encrypted bytes pulled from a live replica.
 */
@Slf4j
@RequiredArgsConstructor
@org.springframework.stereotype.Service
public class SelfHealingService implements NodeManager.NodeStatusListener {

    private final NodeManager nodeManager;
    private final PlacementRegistry registry;
    private final SimpleNodeService simpleNodeService;
    private final EventService eventService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "self-heal");
        t.setDaemon(true);
        return t;
    });

    @PostConstruct
    public void arm() {
        nodeManager.addNodeStatusListener(this);
        log.info("Self-healing service armed — will re-replicate on node failure");
    }

    @PreDestroy
    public void disarm() {
        executor.shutdownNow();
    }

    @Override
    public void onNodeFailure(String nodeId) {
        log.warn("Self-heal triggered by failure of {}", nodeId);
        executor.submit(() -> {
            try {
                heal(nodeId);
            } catch (Exception e) {
                log.error("Self-heal error after {} failure: {}", nodeId, e.getMessage(), e);
            }
        });
    }

    /**
     * When a node rejoins the cluster, re-acknowledge any tracked files whose data is still
     * physically on its disk (a killed node keeps its files — only its server was stopped).
     * This restores placement for under-replicated files without re-copying anything.
     */
    public void resyncNode(String nodeId) {
        Node node = nodeManager.getNodeById(nodeId);
        if (node == null) {
            return;
        }
        int restored = 0;
        for (FilePlacement placement : registry.all()) {
            if (healthyReplicaCount(placement) >= placement.getReplicationFactor()) {
                continue; // already fully replicated
            }
            if (!placement.getNodeIds().contains(nodeId) && node.hasFile(placement.getFilePath())) {
                registry.addReplica(placement.getFilePath(), nodeId);
                restored++;
            }
        }
        if (restored > 0) {
            log.info("Node {} rejoined — re-synced {} replica(s) from its disk", nodeId, restored);
        }
    }

    private void heal(String deadNodeId) {
        List<FilePlacement> affected = registry.filesWithReplicaOn(deadNodeId);
        if (affected.isEmpty()) {
            log.info("No files were hosted on {} — nothing to re-replicate", deadNodeId);
            return;
        }
        log.info("Node {} failed — re-replicating {} affected file(s)", deadNodeId, affected.size());
        eventService.healStarted(deadNodeId, affected.size());
        for (FilePlacement placement : affected) {
            registry.removeReplica(placement.getFilePath(), deadNodeId); // that replica is gone
            restore(placement);
        }
        eventService.healCompleted(deadNodeId);
    }

    private void restore(FilePlacement placement) {
        int guard = 0;
        while (healthyReplicaCount(placement) < placement.getReplicationFactor() && guard++ < 20) {
            Node source = healthySourceWithFile(placement);
            if (source == null) {
                log.warn("No healthy replica of {} remains — cannot restore (potential data loss)", placement.getFileName());
                return;
            }
            Node target = healthyTargetWithout(placement);
            if (target == null) {
                log.warn("No spare healthy node for {} — cluster too small to reach factor {}",
                        placement.getFileName(), placement.getReplicationFactor());
                return;
            }

            byte[] data = simpleNodeService.retrieveFileFromNode(source, placement.getFilePath());
            if (data == null) {
                log.warn("Source {} returned no data for {} — aborting restore", source.getNodeId(), placement.getFileName());
                return;
            }

            if (simpleNodeService.storeFileOnNode(target, placement.getFilePath(), data)) {
                target.addHostedFile(placement.getFilePath());
                registry.addReplica(placement.getFilePath(), target.getNodeId());
                eventService.replicaRestored(placement.getFileName(), target.getNodeId(), source.getNodeId(), placement.getUserId());
                log.info("↳ restored replica of {} onto {} (copied from {})",
                        placement.getFileName(), target.getNodeId(), source.getNodeId());
            } else {
                log.warn("Failed to store replica of {} onto {}", placement.getFileName(), target.getNodeId());
                return;
            }
        }

        int replicas = healthyReplicaCount(placement);
        if (replicas >= placement.getReplicationFactor()) {
            log.info("✓ {} restored to {}/{} replicas", placement.getFileName(), replicas, placement.getReplicationFactor());
        }
    }

    private int healthyReplicaCount(FilePlacement placement) {
        return (int) placement.getNodeIds().stream().filter(nodeManager::isNodeHealthy).count();
    }

    private Node healthySourceWithFile(FilePlacement placement) {
        return placement.getNodeIds().stream()
                .map(nodeManager::getNodeById)
                .filter(n -> n != null && n.isHealthy())
                .findFirst()
                .orElse(null);
    }

    private Node healthyTargetWithout(FilePlacement placement) {
        return nodeManager.getHealthyNodes().stream()
                .filter(n -> !placement.getNodeIds().contains(n.getNodeId()))
                .findFirst()
                .orElse(null);
    }
}
