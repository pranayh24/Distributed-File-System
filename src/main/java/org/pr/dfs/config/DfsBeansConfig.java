package org.pr.dfs.config;

import lombok.RequiredArgsConstructor;
import org.pr.dfs.replication.FaultToleranceManager;
import org.pr.dfs.model.Node;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.replication.ReplicationManager;
import org.pr.dfs.utils.MetricsCollector;
import org.pr.dfs.versioning.VersionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DfsBeansConfig {

    @Autowired
    private final DfsConfig dfsConfig;

    @Bean
    public NodeManager getNodeManager() {
        NodeManager nodeManager = new NodeManager();
        // Initialize with default nodes based on your storage structure
        initializeDefaultNodes(nodeManager);
        return nodeManager;
    }

    private void initializeDefaultNodes(NodeManager nodeManager) {
        try {
            // Register localhost nodes on different ports for realistic testing
            Node node1 = new Node("localhost", 8091);
            node1.setNodeId("node_localhost_8091");

            Node node2 = new Node("localhost", 8092);
            node2.setNodeId("node_localhost_8092");

            Node node3 = new Node("localhost", 8093);
            node3.setNodeId("node_localhost_8093");

            nodeManager.registerNode(node1);
            nodeManager.registerNode(node2);
            nodeManager.registerNode(node3);

            System.out.println("Initialized distributed nodes: " + nodeManager.getAllNodes().size());
        } catch (Exception e) {
            System.err.println("Failed to initialize nodes: " + e.getMessage());
        }
    }

    @Bean
    public ReplicationManager replicationManager(NodeManager nodeManager) {
        return new ReplicationManager(dfsConfig.getReplication().getFactor(), nodeManager);
    }

    @Bean
    public FaultToleranceManager faultToleranceManager(NodeManager nodeManager, ReplicationManager replicationManager) {
        return new FaultToleranceManager(nodeManager, replicationManager);
    }

    @Bean
    public VersionManager versionManager() {
        return new VersionManager(dfsConfig.getStorage().getPath());
    }

    @Bean
    public MetricsCollector metricsCollector() {
        return new MetricsCollector();
    }
}
