package org.pr.dfs.fault;

import org.pr.dfs.model.Node;

public interface NodeFailureHandler {
    void handleNodeFailure(Node failedNode);
}
