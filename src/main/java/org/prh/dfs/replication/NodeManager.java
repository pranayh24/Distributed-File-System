package org.prh.dfs.replication;

import org.prh.dfs.model.Node;

import java.util.ArrayList;
import java.util.List;

public class NodeManager {
    public void registerNode(String address, int port) { }

    public void deregisterNode(String nodeId) {

    }

    public Node electPrimaryNode() {
        return null;
    }

    public List<Node> getHealthyNodes() {
        return new ArrayList<>();
    }

    public void updateNodeStatus(String nodeId, NodeStatus status) {

    }
}
