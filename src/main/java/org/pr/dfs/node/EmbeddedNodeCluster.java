package org.pr.dfs.node;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.service.EventService;
import org.pr.dfs.service.SelfHealingService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Launches the storage-node cluster in-process, alongside the main application,
 * so the whole distributed system runs as a single deployable unit.
 *
 * <p>Each node is a real {@link SimpleHTTPNodeServer} listening on its own port with its
 * own on-disk storage directory — replication genuinely crosses independent servers.
 * Nodes can be killed and revived at runtime to demonstrate fault tolerance and recovery.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddedNodeCluster {

    private final DfsConfig dfsConfig;
    private final NodeManager nodeManager;
    private final EventService eventService;
    private final SelfHealingService selfHealingService;

    private final Map<String, SimpleHTTPNodeServer> servers = new ConcurrentHashMap<>();
    private final Map<String, Integer> ports = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void startCluster() {
        int count = dfsConfig.getCluster().getNodeCount();
        int basePort = dfsConfig.getCluster().getBasePort();

        log.info("Starting embedded storage-node cluster: {} nodes from port {}", count, basePort);

        for (int i = 1; i <= count; i++) {
            String nodeId = "node" + i;
            int port = basePort + (i - 1);
            ports.put(nodeId, port);
            try {
                startNodeServer(nodeId, port);

                Node node = new Node("localhost", port);
                node.setNodeId(nodeId);
                node.setStoragePath("./storage/" + nodeId);
                nodeManager.registerNode(node);

                log.info("  ✓ {} online at localhost:{}", nodeId, port);
            } catch (Exception e) {
                log.error("  ✗ Failed to start {} on port {}: {}", nodeId, port, e.getMessage());
            }
        }

        log.info("Embedded cluster ready: {}/{} nodes registered", servers.size(), count);
    }

    /** Stop a node's HTTP server for real, then mark it unhealthy so the cluster reacts immediately. */
    public boolean stopNode(String nodeId) {
        SimpleHTTPNodeServer server = servers.remove(nodeId);
        if (server == null) {
            log.warn("Cannot kill {} — not running", nodeId);
            return false;
        }
        server.stop();
        eventService.nodeKilled(nodeId);
        nodeManager.markNodeUnhealthy(nodeId);
        log.warn("☠ Node {} killed (server stopped) — replication will now skip it", nodeId);
        return true;
    }

    /** Restart a previously-killed node's HTTP server and mark it healthy again. */
    public boolean startNode(String nodeId) {
        if (servers.containsKey(nodeId)) {
            log.info("Node {} already running", nodeId);
            return true;
        }
        Integer port = ports.get(nodeId);
        if (port == null) {
            log.warn("Cannot revive {} — unknown node", nodeId);
            return false;
        }
        try {
            startNodeServer(nodeId, port);
            nodeManager.markNodeHealthy(nodeId);
            selfHealingService.resyncNode(nodeId); // re-acknowledge files still on its disk
            eventService.nodeRevived(nodeId);
            log.info("↻ Node {} revived at localhost:{}", nodeId, port);
            return true;
        } catch (Exception e) {
            log.error("Failed to revive {}: {}", nodeId, e.getMessage());
            return false;
        }
    }

    /** Snapshot of which nodes are currently running (server up) vs killed. */
    public Map<String, Boolean> runningState() {
        Map<String, Boolean> state = new LinkedHashMap<>();
        ports.keySet().stream().sorted().forEach(id -> state.put(id, servers.containsKey(id)));
        return state;
    }

    private void startNodeServer(String nodeId, int port) throws Exception {
        SimpleHTTPNodeServer server = new SimpleHTTPNodeServer(nodeId, port);
        server.start();
        servers.put(nodeId, server);
    }

    @PreDestroy
    public void stopCluster() {
        log.info("Stopping embedded storage-node cluster ({} nodes)", servers.size());
        servers.values().forEach(SimpleHTTPNodeServer::stop);
        servers.clear();
    }
}
