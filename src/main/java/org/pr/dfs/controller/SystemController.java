package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.service.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
@Tag(name = "System Operations", description = "APIs for system monitoring and management")
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/health")
    @Operation(summary = "System health check", description = "Get the health status of the DFS system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        try {
            log.debug("Getting system health status");

            Map<String, Object> healthStatus = systemService.getSystemHealth();

            return ResponseEntity.ok(ApiResponse.success(healthStatus));

        } catch (Exception e) {
            log.error("Error getting system health: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get system health: " + e.getMessage()));
        }
    }

    @GetMapping("/nodes")
    @Operation(summary = "Get node information", description = "Get information about all nodes in the system")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNodeInfo() {
        try {
            log.debug("Getting node information");

            Map<String, Object> nodeInfo = systemService.getNodeInfo();

            return ResponseEntity.ok(ApiResponse.success(nodeInfo));

        } catch (Exception e) {
            log.error("Error getting node info: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get node information: " + e.getMessage()));
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get system metrics", description = "Get performance and usage metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemMetrics() {
        try {
            log.debug("Getting system metrics");

            Map<String, Object> metrics = systemService.getSystemMetrics();

            return ResponseEntity.ok(ApiResponse.success(metrics));

        } catch (Exception e) {
            log.error("Error getting system metrics: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to get system metrics: " + e.getMessage()));
        }
    }

    @PostMapping("/nodes/{nodeId}/recover")
    @Operation(summary = "Recover failed node", description = "Initiate recovery process for a failed node")
    public ResponseEntity<ApiResponse<Void>> recoverNode(@PathVariable String nodeId) {
        try {
            log.info("Initiating recovery for node: {}", nodeId);

            systemService.recoverNode(nodeId);

            return ResponseEntity.ok(ApiResponse.success("Node recovery initiated", null));

        } catch (Exception e) {
            log.error("Error recovering node {}: {}", nodeId, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to recover node: " + e.getMessage()));
        }
    }
}