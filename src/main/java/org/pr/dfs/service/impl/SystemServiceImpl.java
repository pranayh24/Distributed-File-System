package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.FaultToleranceManager;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.service.SystemService;
import org.pr.dfs.utils.MetricsCollector;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final DfsConfig dfsConfig;
    private final NodeManager nodeManager;
    private final FaultToleranceManager faultToleranceManager;
    private final MetricsCollector metricsCollector;

    @Override
    public Map<String, Object> getSystemHealth() throws Exception {
        log.debug("Getting system health status");

        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());

        List<Node> allNodes = nodeManager.getAllNodes();
        List<Node> healthyNodes = nodeManager.getHealthyNodes();

        Map<String, Object> nodeHealth = new HashMap<>();
        nodeHealth.put("totalNodes", allNodes.size());
        nodeHealth.put("healthyNodes", healthyNodes.size());
        nodeHealth.put("unhealthyNodes", allNodes.size() - healthyNodes.size());
        nodeHealth.put("healthPercentage", allNodes.isEmpty() ? 0 :
                (double) healthyNodes.size() / allNodes.size() * 100);

        health.put("nodes", nodeHealth);

        Map<String, Object> storage = new HashMap<>();
        File storageDir = new File(dfsConfig.getStorage().getPath());
        storage.put("path", dfsConfig.getStorage().getPath());
        storage.put("exists", storageDir.exists());
        storage.put("writable", storageDir.canWrite());
        storage.put("totalSpace", storageDir.getTotalSpace());
        storage.put("freeSpace", storageDir.getFreeSpace());
        storage.put("usableSpace", storageDir.getUsableSpace());

        health.put("storage", storage);

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
        memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
        memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());

        health.put("memory", memory);

        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> system = new HashMap<>();
        system.put("loadAverage", osBean.getSystemLoadAverage());
        system.put("availableProcessors", osBean.getAvailableProcessors());

        health.put("system", system);

        return health;
    }

    @Override
    public Map<String, Object> getNodeInfo() throws Exception {
        log.debug("Getting node information");

        Map<String, Object> nodeInfo = new HashMap<>();

        List<Node> allNodes = nodeManager.getAllNodes();

        List<Map<String, Object>> nodeDetails = allNodes.stream()
                .map(this::convertNodeToMap)
                .collect(Collectors.toList());

        nodeInfo.put("totalNodes", allNodes.size());
        nodeInfo.put("nodes", nodeDetails);

        long healthyCount = allNodes.stream().mapToLong(n -> n.isHealthy() ? 1 : 0).sum();
        nodeInfo.put("healthyNodes", healthyCount);
        nodeInfo.put("unhealthyNodes", allNodes.size() - healthyCount);

        long totalFiles = allNodes.stream()
                .mapToLong(n -> n.getHostedFiles() != null ? n.getHostedFiles().size() : 0)
                .sum();
        nodeInfo.put("totalFilesHosted", totalFiles);

        return nodeInfo;
    }

    @Override
    public Map<String, Object> getSystemMetrics() throws Exception {
        log.debug("Getting system metrics");

        Map<String, Object> metrics = new HashMap<>();

        metricsCollector.collectSystemMetrics();

        Map<String, Object> fileOps = new HashMap<>();

        fileOps.put("uploadsToday", 0);
        fileOps.put("downloadsToday", 0);
        fileOps.put("deletionsToday", 0);

        metrics.put("fileOperations", fileOps);

        Map<String, Object> replication = new HashMap<>();
        replication.put("successfulReplications", 0);
        replication.put("failedReplications", 0);
        replication.put("averageReplicationTime", 0);

        metrics.put("replication", replication);

        Map<String, Object> performance = new HashMap<>();
        performance.put("averageResponseTime", 0);
        performance.put("throughput", 0);
        performance.put("errorRate", 0);

        metrics.put("performance", performance);

        List<Node> nodes = nodeManager.getAllNodes();
        Map<String, Object> nodeMetrics = new HashMap<>();
        nodeMetrics.put("totalNodes", nodes.size());
        nodeMetrics.put("activeNodes", nodeManager.getHealthyNodes().size());
        nodeMetrics.put("totalDiskSpace", nodes.stream()
                .mapToLong(Node::getAvailableDiskSpace)
                .sum());

        metrics.put("nodes", nodeMetrics);

        metrics.put("timestamp", LocalDateTime.now());

        return metrics;
    }

    @Override
    public void recoverNode(String nodeId) throws Exception {
        log.info("Initiating recovery for node: {}", nodeId);

        Node node = nodeManager.getNodeById(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeId);
        }

        faultToleranceManager.initiateRecovery(nodeId)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        log.info("Recovery completed for node {}: {}", nodeId, result.getMessage());
                    } else {
                        log.error("Recovery failed for node {}: {}", nodeId, result.getMessage());
                    }
                })
                .exceptionally(throwable -> {
                    log.error("Recovery process failed for node {}: {}", nodeId, throwable.getMessage());
                    return null;
                });

        log.info("Recovery process initiated for node: {}", nodeId);
    }

    private Map<String, Object> convertNodeToMap(Node node) {
        Map<String, Object> nodeMap = new HashMap<>();
        nodeMap.put("nodeId", node.getNodeId());
        nodeMap.put("address", node.getAddress());
        nodeMap.put("port", node.getPort());
        nodeMap.put("healthy", node.isHealthy());
        nodeMap.put("availableDiskSpace", node.getAvailableDiskSpace());
        nodeMap.put("lastHeartbeat", node.getLastHeartbeat());
        nodeMap.put("hostedFilesCount", node.getHostedFiles() != null ? node.getHostedFiles().size() : 0);
        nodeMap.put("storagePath", node.getStoragePath());

        return nodeMap;
    }
}