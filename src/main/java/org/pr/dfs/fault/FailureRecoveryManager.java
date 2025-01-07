/**package org.pr.dfs.fault;

import org.pr.dfs.model.Node;
import org.pr.dfs.replication.ReplicationManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FailureRecoveryManager implements NodeFailureHandler{
    private static final Logger LOGGER = Logger.getLogger(FailureRecoveryManager.class.getName());

    private final ReplicationManager replicationManager;
    private final Map<String, Set<String>> nodeToFilesMap; // Track files on each node

    public FailureRecoveryManager(ReplicationManager replicationManager) {
        this.replicationManager = replicationManager;
        this.nodeToFilesMap = new ConcurrentHashMap<>();
    }

    @Override
    public void handleNodeFailure(Node failedNode) {
        LOGGER.warning("Handling failure of node: " + failedNode.getNodeId());

        // Get all files that were on the failed node
        Set<String> affectedFiles = nodeToFilesMap.getOrDefault(failedNode.getNodeId(), new HashSet<>());

        // Trigger re-replication for each affected file
        for (String fileName : affectedFiles) {
            recoverFile(fileName, failedNode);
        }

        // Remove the failed node from our tracking
        nodeToFilesMap.remove(failedNode.getNodeId());
    }

    private void recoverFile(String fileName, Node failedNode) {
        try {
            // Get file data from a healthy replica
            byte[] fileData = getFileFromHealthyReplica(fileName);
            if(fileData != null) {
                // Trigger Re-replication
                replicationManager.replicateFile(fileName, fileData)
                        .thenAccept(success -> {
                            if(success) {
                                LOGGER.info("Successfully recovered file: " + fileName);
                            } else {
                                LOGGER.severe("Failed to recover file: " + fileName);
                            }
                        });
            }
        } catch(Exception e) {
            LOGGER.severe("Error during file recovery: " + e.getMessage());
        }
    }

    private byte[] getFileFromHealthyReplica(String fileName) {
        // todo
        return null;
    }

    public void trackFile(String nodeId, String fileName) {
        nodeToFilesMap.computeIfAbsent(nodeId, k-> new HashSet<>()).add(fileName);
    }
}
