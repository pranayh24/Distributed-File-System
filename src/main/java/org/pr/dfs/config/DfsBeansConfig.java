package org.pr.dfs.config;

import lombok.RequiredArgsConstructor;
import org.pr.dfs.replication.FaultToleranceManager;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.replication.ReplicationManager;
import org.pr.dfs.utils.MetricsCollector;
import org.pr.dfs.versioning.VersionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DfsBeansConfig {

    private final DfsConfig dfsConfig;

    @Bean
    public NodeManager getNodeManager() {
        return new NodeManager();
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
