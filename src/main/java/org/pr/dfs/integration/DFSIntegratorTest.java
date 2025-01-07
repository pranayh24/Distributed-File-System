package org.pr.dfs.integration;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import java.util.concurrent.TimeUnit;

public class DFSIntegratorTest {
    private DFSIntegrator dfsIntegrator;

    @Before
    public void setup() {
        dfsIntegrator = new DFSIntegrator();
        dfsIntegrator.initializeSystem();
    }

    @Test
    public void testReplicationAndFaultTolerance() throws Exception {
        // Register nodes
        String node1 = dfsIntegrator.addNode("192.168.1.1", 8001);
        String node2 = dfsIntegrator.addNode("192.168.1.2", 8002);
        String node3 = dfsIntegrator.addNode("192.168.1.3", 8003);

        // Test file writing with replication
        String testFileName = "test.txt";
        byte[] testData = "Hello, Distributed World!".getBytes();

        boolean writeSuccess = dfsIntegrator.writeFile(testFileName, testData)
                .get(5, TimeUnit.SECONDS);
        assertTrue("File write should succeed", writeSuccess);

        // Simulate node failure
        dfsIntegrator.handleNodeFailure(node1);

        // Wait for recovery to complete
        Thread.sleep(2000);

        // Simulate heartbeat from remaining nodes
        dfsIntegrator.handleHeartbeat(node2);
        dfsIntegrator.handleHeartbeat(node3);

        // Add a new node to maintain replication factor
        String node4 = dfsIntegrator.addNode("192.168.1.4", 8004);

        // Wait for re-replication
        Thread.sleep(2000);

        // Clean up
        dfsIntegrator.shutdown();
    }
}