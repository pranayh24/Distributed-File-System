package org.pr.dfs.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.FaultToleranceManager;
import org.pr.dfs.replication.NodeManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthMonitoringService {

    private final NodeManager nodeManager;
    private final FaultToleranceManager faultToleranceManager;
    private final SimpleNodeService simpleNodeService; // Added simple node service for HTTP health checks

    private final AtomicInteger healthCheckCount = new AtomicInteger(0);
    private volatile boolean monitoringActive = true;

    @PostConstruct
    public void startMonitoring() {
        log.info("Health Monitoring Service started - monitoring {} nodes",
                nodeManager.getAllNodes().size());
        monitoringActive = true;
    }

    @PreDestroy
    public void stopMonitoring() {
        log.info("Health Monitoring Service stopped");
        monitoringActive = false;
    }

    /**
     * Performs health checks on all nodes every 30 seconds using simple HTTP
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void performHealthChecks() {
        if (!monitoringActive) {
            return;
        }

        List<Node> allNodes = nodeManager.getAllNodes();
        int healthCheckId = healthCheckCount.incrementAndGet();

        log.debug("Starting health check #{} for {} nodes", healthCheckId, allNodes.size());

        int healthyCount = 0;
        int unhealthyCount = 0;

        for (Node node : allNodes) {
            try {
                // Use SimpleNodeService for HTTP-based health checks
                boolean isHealthy = simpleNodeService.performHealthCheck(node);

                if (isHealthy) {
                    healthyCount++;
                    log.debug("Node {} is healthy", node.getNodeId());
                } else {
                    unhealthyCount++;
                    log.warn("Node {} is unhealthy", node.getNodeId());

                    // Trigger fault tolerance if node becomes unhealthy
                    try {
                        faultToleranceManager.handleNodeFailure(node.getNodeId());
                    } catch (Exception e) {
                        log.error("Failed to handle node failure for {}: {}", node.getNodeId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                unhealthyCount++;
                log.error("Health check failed for node {}: {}", node.getNodeId(), e.getMessage());

                // Mark node as unhealthy
                nodeManager.markNodeUnhealthy(node.getNodeId());

                // Trigger fault tolerance
                try {
                    faultToleranceManager.handleNodeFailure(node.getNodeId());
                } catch (Exception faultException) {
                    log.error("Failed to handle node failure for {}: {}", node.getNodeId(), faultException.getMessage());
                }
            }
        }

        log.info("Health check #{} completed - Healthy: {}, Unhealthy: {}, Total: {}",
                healthCheckId, healthyCount, unhealthyCount, allNodes.size());
    }

    /**
     * Get cluster health status
     */
    public ClusterHealthStatus getClusterHealthStatus() {
        List<Node> allNodes = nodeManager.getAllNodes();
        List<Node> healthyNodes = nodeManager.getHealthyNodes();

        return new ClusterHealthStatus(
                allNodes.size(),
                healthyNodes.size(),
                allNodes.size() - healthyNodes.size(),
                healthCheckCount.get()
        );
    }

    /**
     * Manual health check for a specific node
     */
    public boolean checkNodeHealth(String nodeId) {
        Node node = nodeManager.getNodeById(nodeId);
        if (node == null) {
            log.warn("Node not found for health check: {}", nodeId);
            return false;
        }

        try {
            return simpleNodeService.performHealthCheck(node);
        } catch (Exception e) {
            log.error("Manual health check failed for node {}: {}", nodeId, e.getMessage());
            return false;
        }
    }

    /**
     * Cluster health status data class
     */
    public static class ClusterHealthStatus {
        private final int totalNodes;
        private final int healthyNodes;
        private final int unhealthyNodes;
        private final int totalHealthChecks;

        public ClusterHealthStatus(int totalNodes, int healthyNodes, int unhealthyNodes, int totalHealthChecks) {
            this.totalNodes = totalNodes;
            this.healthyNodes = healthyNodes;
            this.unhealthyNodes = unhealthyNodes;
            this.totalHealthChecks = totalHealthChecks;
        }

        public int getTotalNodes() { return totalNodes; }
        public int getHealthyNodes() { return healthyNodes; }
        public int getUnhealthyNodes() { return unhealthyNodes; }
        public int getTotalHealthChecks() { return totalHealthChecks; }
        public double getHealthPercentage() {
            return totalNodes > 0 ? (double) healthyNodes / totalNodes * 100 : 0;
        }
    }
}
