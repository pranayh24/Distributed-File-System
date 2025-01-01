package org.prh.dfs.fault;

import org.prh.dfs.model.Node;

public interface NodeFailureHandler {
    void handleNodeFailure(Node failedNode);
}
