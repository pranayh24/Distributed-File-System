package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.service.HealthMonitoringService;
import org.pr.dfs.service.SystemService;
import org.pr.dfs.service.SimpleNodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@Tag(name = "System Operations", description = "Consolidated APIs for system monitoring and management")
public class SystemController {

    private final SystemService systemService;
    private final NodeManager nodeManager;
    private final HealthMonitoringService healthMonitoringService;
    private final SimpleNodeService simpleNodeService; // Added missing dependency

    // ===========================================
    // SYSTEM HEALTH & METRICS
    // ===========================================

    @GetMapping("/health")
    @Operation(summary = "System health check", description = "Get comprehensive health status of the DFS system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            log.debug("Getting comprehensive system health status");

            // Get basic system health
            Map<String, Object> healthStatus = systemService.getSystemHealth();

            // Add detailed cluster health
            HealthMonitoringService.ClusterHealthStatus clusterHealth = healthMonitoringService.getClusterHealthStatus();
            healthStatus.put("clusterHealth", Map.of(
                "totalNodes", clusterHealth.getTotalNodes(),
                "healthyNodes", clusterHealth.getHealthyNodes(),
                "unhealthyNodes", clusterHealth.getUnhealthyNodes(),
                "healthPercentage", clusterHealth.getHealthPercentage(),
                "totalHealthChecks", clusterHealth.getTotalHealthChecks()
            ));

            return ResponseEntity.ok(ApiResponse.success("System health retrieved successfully", healthStatus));

        } catch (Exception e) {
            log.error("Error getting system health: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get system health: " + e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics", description = "Get performance and usage metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemMetrics() {
        try {
            log.debug("Getting system metrics");

            Map<String, Object> metrics = systemService.getSystemMetrics();

            return ResponseEntity.ok(ApiResponse.success("System metrics retrieved successfully", metrics));

        } catch (Exception e) {
            log.error("Error getting system metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get system metrics: " + e.getMessage()));
        }
    }

    // ===========================================
    // NODE MANAGEMENT
    // ===========================================

    @GetMapping("/nodes")
    @Operation(summary = "Get all nodes status", description = "Get detailed status of all nodes in the system")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllNodes() {
        try {
            log.debug("Getting all nodes information");

            List<Node> allNodes = nodeManager.getAllNodes();
            List<Map<String, Object>> nodeDetails = allNodes.stream()
                    .map(this::createNodeDetails)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Nodes retrieved successfully", nodeDetails));

        } catch (Exception e) {
            log.error("Error getting nodes: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get nodes: " + e.getMessage()));
        }
    }

    @GetMapping("/nodes/healthy")
    @Operation(summary = "Get healthy nodes", description = "Get only healthy nodes in the system")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHealthyNodes() {
        try {
            List<Node> healthyNodes = nodeManager.getHealthyNodes();
            List<Map<String, Object>> nodeDetails = healthyNodes.stream()
                    .map(this::createNodeDetails)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Healthy nodes retrieved successfully", nodeDetails));

        } catch (Exception e) {
            log.error("Error getting healthy nodes: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get healthy nodes: " + e.getMessage()));
        }
    }

    @GetMapping("/nodes/{nodeId}")
    @Operation(summary = "Get specific node details", description = "Get detailed information about a specific node")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNodeDetails(@PathVariable String nodeId) {
        try {
            Node node = nodeManager.getNodeById(nodeId);

            if (node == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> nodeDetails = createNodeDetails(node);

            return ResponseEntity.ok(ApiResponse.success("Node details retrieved successfully", nodeDetails));

        } catch (Exception e) {
            log.error("Error getting node details for {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get node details: " + e.getMessage()));
        }
    }

    // ===========================================
    // HEALTH CHECKS & RECOVERY
    // ===========================================

    @PostMapping("/nodes/{nodeId}/health-check")
    @Operation(summary = "Trigger manual health check", description = "Manually trigger health check for a specific node")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerNodeHealthCheck(@PathVariable String nodeId) {
        try {
            Node node = nodeManager.getNodeById(nodeId);

            if (node == null) {
                return ResponseEntity.notFound().build();
            }

            boolean wasHealthy = node.isHealthy();
            boolean isHealthy = healthMonitoringService.checkNodeHealth(nodeId);

            Map<String, Object> result = new HashMap<>();
            result.put("nodeId", nodeId);
            result.put("wasHealthy", wasHealthy);
            result.put("isHealthy", isHealthy);
            result.put("statusChanged", wasHealthy != isHealthy);
            result.put("lastHeartbeat", node.getLastHeartbeat());

            return ResponseEntity.ok(ApiResponse.success("Health check completed", result));

        } catch (Exception e) {
            log.error("Error performing health check for node {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to perform health check: " + e.getMessage()));
        }
    }

    @PostMapping("/health-check")
    @Operation(summary = "Trigger system-wide health check", description = "Manually trigger health check for all nodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSystemHealthCheck() {
        try {
            List<Node> allNodes = nodeManager.getAllNodes();
            int totalNodes = allNodes.size();
            int healthyNodes = 0;
            int unhealthyNodes = 0;

            for (Node node : allNodes) {
                boolean isHealthy = healthMonitoringService.checkNodeHealth(node.getNodeId());
                if (isHealthy) {
                    healthyNodes++;
                } else {
                    unhealthyNodes++;
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalNodes", totalNodes);
            result.put("healthyNodes", healthyNodes);
            result.put("unhealthyNodes", unhealthyNodes);
            result.put("healthPercentage", totalNodes > 0 ? (double) healthyNodes / totalNodes * 100 : 0);

            return ResponseEntity.ok(ApiResponse.success("System health check completed", result));

        } catch (Exception e) {
            log.error("Error triggering system health check: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to trigger health check: " + e.getMessage()));
        }
    }

    @PostMapping("/nodes/{nodeId}/recover")
    @Operation(summary = "Recover failed node", description = "Initiate recovery process for a failed node")
    public ResponseEntity<ApiResponse<String>> recoverNode(@PathVariable String nodeId) {
        try {
            log.info("Initiating recovery for node: {}", nodeId);

            systemService.recoverNode(nodeId);

            return ResponseEntity.ok(ApiResponse.success("Node recovery initiated",
                "Recovery process has been started for node: " + nodeId));

        } catch (Exception e) {
            log.error("Error recovering node {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to recover node: " + e.getMessage()));
        }
    }

    // ===========================================
    // ADMIN OPERATIONS
    // ===========================================

    @PostMapping("/nodes/{nodeId}/mark-healthy")
    @Operation(summary = "Manually mark node as healthy", description = "Force mark a node as healthy (admin operation)")
    public ResponseEntity<ApiResponse<String>> markNodeHealthy(@PathVariable String nodeId) {
        try {
            Node node = nodeManager.getNodeById(nodeId);

            if (node == null) {
                return ResponseEntity.notFound().build();
            }

            nodeManager.markNodeHealthy(nodeId);

            return ResponseEntity.ok(ApiResponse.success("Node marked as healthy",
                    "Node " + nodeId + " has been manually marked as healthy"));

        } catch (Exception e) {
            log.error("Error marking node {} as healthy: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to mark node as healthy: " + e.getMessage()));
        }
    }

    @PostMapping("/nodes/{nodeId}/mark-unhealthy")
    @Operation(summary = "Manually mark node as unhealthy", description = "Force mark a node as unhealthy (admin operation)")
    public ResponseEntity<ApiResponse<String>> markNodeUnhealthy(@PathVariable String nodeId) {
        try {
            Node node = nodeManager.getNodeById(nodeId);

            if (node == null) {
                return ResponseEntity.notFound().build();
            }

            nodeManager.markNodeUnhealthy(nodeId);

            return ResponseEntity.ok(ApiResponse.success("Node marked as unhealthy",
                    "Node " + nodeId + " has been manually marked as unhealthy"));

        } catch (Exception e) {
            log.error("Error marking node {} as unhealthy: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to mark node as unhealthy: " + e.getMessage()));
        }
    }

    @GetMapping("/replication/status")
    @Operation(summary = "Get replication status", description = "Get replication status for the system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReplicationStatus() {
        try {
            Map<String, Object> replicationInfo = new HashMap<>();
            replicationInfo.put("healthyNodes", nodeManager.getHealthyNodes().size());
            replicationInfo.put("totalNodes", nodeManager.getAllNodes().size());
            replicationInfo.put("replicationHealthy", nodeManager.getHealthyNodes().size() >= 2);

            return ResponseEntity.ok(ApiResponse.success("Replication status retrieved", replicationInfo));

        } catch (Exception e) {
            log.error("Error getting replication status: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get replication status: " + e.getMessage()));
        }
    }

    // ===========================================
    // ADMIN FILE TRACKING & DISTRIBUTION
    // ===========================================

    @GetMapping("/files/distribution")
    @Operation(summary = "Get file distribution across nodes", description = "Get information about how files are distributed across all nodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileDistribution() {
        try {
            Map<String, Object> distribution = new HashMap<>();
            List<Node> allNodes = nodeManager.getAllNodes();

            Map<String, Object> nodeDistribution = new HashMap<>();
            int totalFiles = 0;

            for (Node node : allNodes) {
                Map<String, Object> nodeInfo = new HashMap<>();
                nodeInfo.put("nodeId", node.getNodeId());
                nodeInfo.put("address", node.getAddress());
                nodeInfo.put("port", node.getPort());
                nodeInfo.put("healthy", node.isHealthy());
                nodeInfo.put("fileCount", node.getHostedFiles().size());
                nodeInfo.put("hostedFiles", new ArrayList<>(node.getHostedFiles()));

                nodeDistribution.put(node.getNodeId(), nodeInfo);
                totalFiles += node.getHostedFiles().size();
            }

            distribution.put("nodes", nodeDistribution);
            distribution.put("totalNodes", allNodes.size());
            distribution.put("healthyNodes", nodeManager.getHealthyNodes().size());
            distribution.put("totalFileInstances", totalFiles);
            distribution.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(ApiResponse.success("File distribution retrieved successfully", distribution));

        } catch (Exception e) {
            log.error("Error getting file distribution: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get file distribution: " + e.getMessage()));
        }
    }

    @GetMapping("/files/locations")
    @Operation(summary = "Find file locations", description = "Find which nodes contain a specific file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileLocations(@RequestParam String filePath) {
        try {
            Map<String, Object> result = new HashMap<>();
            List<Node> allNodes = nodeManager.getAllNodes();
            List<Map<String, Object>> locations = new ArrayList<>();

            for (Node node : allNodes) {
                if (node.getHostedFiles().contains(filePath)) {
                    Map<String, Object> location = new HashMap<>();
                    location.put("nodeId", node.getNodeId());
                    location.put("address", node.getAddress());
                    location.put("port", node.getPort());
                    location.put("healthy", node.isHealthy());
                    location.put("lastHeartbeat", node.getLastHeartbeat());
                    locations.add(location);
                }
            }

            result.put("filePath", filePath);
            result.put("foundOnNodes", locations);
            result.put("replicationCount", locations.size());
            result.put("isFullyReplicated", locations.size() >= 2); // Assuming min replication is 2

            return ResponseEntity.ok(ApiResponse.success("File locations retrieved successfully", result));

        } catch (Exception e) {
            log.error("Error getting file locations for {}: {}", filePath, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get file locations: " + e.getMessage()));
        }
    }

    @GetMapping("/nodes/{nodeId}/files")
    @Operation(summary = "Get files on specific node", description = "Get list of all files stored on a specific node")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNodeFiles(@PathVariable String nodeId) {
        try {
            Node node = nodeManager.getNodeById(nodeId);

            if (node == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> result = new HashMap<>();
            result.put("nodeId", nodeId);
            result.put("address", node.getAddress());
            result.put("port", node.getPort());
            result.put("healthy", node.isHealthy());
            result.put("fileCount", node.getHostedFiles().size());
            result.put("hostedFiles", new ArrayList<>(node.getHostedFiles()));
            result.put("lastHeartbeat", node.getLastHeartbeat());

            // Get detailed file info if node is healthy
            if (node.isHealthy()) {
                try {
                    String nodeInfoJson = simpleNodeService.getNodeInfo(node);
                    if (nodeInfoJson != null) {
                        result.put("detailedNodeInfo", nodeInfoJson);
                    }
                } catch (Exception e) {
                    log.warn("Failed to get detailed info from node {}: {}", nodeId, e.getMessage());
                }
            }

            return ResponseEntity.ok(ApiResponse.success("Node files retrieved successfully", result));

        } catch (Exception e) {
            log.error("Error getting files for node {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get node files: " + e.getMessage()));
        }
    }

    @GetMapping("/storage/summary")
    @Operation(summary = "Get storage summary", description = "Get comprehensive storage summary across all nodes")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStorageSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            List<Node> allNodes = nodeManager.getAllNodes();
            List<Node> healthyNodes = nodeManager.getHealthyNodes();

            // Calculate totals
            int totalFileInstances = 0;
            Set<String> uniqueFiles = new HashSet<>();
            Map<String, Integer> fileReplicationCount = new HashMap<>();

            for (Node node : allNodes) {
                totalFileInstances += node.getHostedFiles().size();
                for (String filePath : node.getHostedFiles()) {
                    uniqueFiles.add(filePath);
                    fileReplicationCount.merge(filePath, 1, Integer::sum);
                }
            }

            // Find under-replicated files
            List<Map<String, Object>> underReplicatedFiles = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : fileReplicationCount.entrySet()) {
                if (entry.getValue() < 2) { // Assuming min replication is 2
                    Map<String, Object> fileInfo = new HashMap<>();
                    fileInfo.put("filePath", entry.getKey());
                    fileInfo.put("currentReplicas", entry.getValue());
                    fileInfo.put("requiredReplicas", 2);
                    underReplicatedFiles.add(fileInfo);
                }
            }

            summary.put("totalNodes", allNodes.size());
            summary.put("healthyNodes", healthyNodes.size());
            summary.put("unhealthyNodes", allNodes.size() - healthyNodes.size());
            summary.put("uniqueFiles", uniqueFiles.size());
            summary.put("totalFileInstances", totalFileInstances);
            summary.put("averageReplication", uniqueFiles.isEmpty() ? 0 : (double) totalFileInstances / uniqueFiles.size());
            summary.put("underReplicatedFiles", underReplicatedFiles);
            summary.put("underReplicatedCount", underReplicatedFiles.size());
            summary.put("storageHealthy", underReplicatedFiles.isEmpty() && healthyNodes.size() >= 2);

            return ResponseEntity.ok(ApiResponse.success("Storage summary retrieved successfully", summary));

        } catch (Exception e) {
            log.error("Error getting storage summary: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get storage summary: " + e.getMessage()));
        }
    }

    // ===========================================
    // HELPER METHODS
    // ===========================================

    private Map<String, Object> createNodeDetails(Node node) {
        Map<String, Object> details = new HashMap<>();
        details.put("nodeId", node.getNodeId());
        details.put("address", node.getAddress());
        details.put("port", node.getPort());
        details.put("healthy", node.isHealthy());
        details.put("lastHeartbeat", node.getLastHeartbeat());
        details.put("timeSinceLastHeartbeat", System.currentTimeMillis() - node.getLastHeartbeat());
        details.put("hostedFiles", node.getHostedFiles().size());
        details.put("hostedFilesList", node.getHostedFiles());
        details.put("availableDiskSpace", node.getAvailableDiskSpace());
        return details;
    }
}
