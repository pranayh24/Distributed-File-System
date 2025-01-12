package org.pr.dfs.replication;

import lombok.Getter;
import org.pr.dfs.model.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class NodeManager {
    @Getter
    private final ConcurrentHashMap<String, Node> nodes;
    private final List<NodeStatusListener> statusListeners;

    public NodeManager() {
        this.nodes = new ConcurrentHashMap<>();
        this.statusListeners = new ArrayList<>();
    }

    public void registerNode(String nodeId, Node node) {
        nodes.put(nodeId, node);
    }

    public void deregisterNode(String nodeId) {
        nodes.remove(nodeId);
        notifyNodeFailure(nodeId); // Notify listeners

    }

    public List<Node> getHealthyNodes() {
        return new ArrayList<>(nodes.values());
    }

    public Node getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public void addNodeStatusListener(NodeStatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    private void notifyNodeFailure(String nodeId) {
        for (NodeStatusListener listener : statusListeners) {
            listener.onNodeFailure(nodeId);
        }
    }

    // Observer interface
    public interface NodeStatusListener {
        void onNodeFailure(String nodeId);
    }
}
