package org.pr.dfs.model;

import lombok.Builder;
import lombok.Data;

/**
 * A single real event emitted by the distributed system as operations happen,
 * streamed to clients over SSE so the dashboard can visualize the backend live.
 */
@Data
@Builder
public class DfsEvent {

    public enum Type {
        UPLOAD_STARTED,
        ENCRYPTED,
        REPLICA_STORED,
        UPLOAD_COMMITTED,
        NODE_KILLED,
        NODE_REVIVED,
        HEAL_STARTED,
        REPLICA_RESTORED,
        HEAL_COMPLETED,
        FILE_DELETED
    }

    private Type type;

    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /** Owner of a file-related event; null means a global/cluster event visible to everyone. */
    private String userId;

    private String message;
    private String fileName;
    private String nodeId;
    private String sourceNodeId;
    private Integer replicaIndex;
    private Integer replicas;
    private Integer replicationFactor;
    private Long size;
}
