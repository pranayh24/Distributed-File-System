package org.pr.dfs.config;

import org.pr.dfs.model.Node;
import org.pr.dfs.replication.NodeManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class NodeConfiguration {

    private static final Logger LOGGER = Logger.getLogger(NodeConfiguration.class.getName());

    @Autowired
    private NodeManager nodeManager;

    @EventListener(ApplicationReadyEvent.class)
    public void registerNodes() {
        LOGGER.info("Registering distributed nodes with NodeManager...");

        // Register the simple HTTP nodes - fix constructor to match Node model
        Node node1 = new Node("localhost", 8091);
        node1.setNodeId("node1");

        Node node2 = new Node("localhost", 8092);
        node2.setNodeId("node2");

        Node node3 = new Node("localhost", 8093);
        node3.setNodeId("node3");

        nodeManager.registerNode(node1);
        nodeManager.registerNode(node2);
        nodeManager.registerNode(node3);

        LOGGER.info("Successfully registered 3 nodes with the distributed file system");
        LOGGER.info("Node 1: localhost:8091");
        LOGGER.info("Node 2: localhost:8092");
        LOGGER.info("Node 3: localhost:8093");
    }
}
