package org.pr.dfs.service;

import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.DfsEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Broadcasts {@link DfsEvent}s to connected SSE clients and keeps a short replay buffer.
 * File-related events carry an owner {@code userId} and are only delivered to that user;
 * cluster events (node kill/heal) have a null userId and are visible to everyone.
 */
@Slf4j
@Service
public class EventService {

    private static final int MAX_RECENT = 60;

    private final Map<SseEmitter, String> subscribers = new ConcurrentHashMap<>();
    private final Deque<DfsEvent> recent = new ConcurrentLinkedDeque<>();

    public SseEmitter subscribe(String userId) {
        String uid = userId == null ? "" : userId;
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        subscribers.put(emitter, uid);
        emitter.onCompletion(() -> subscribers.remove(emitter));
        emitter.onTimeout(() -> {
            subscribers.remove(emitter);
            emitter.complete();
        });
        emitter.onError(e -> subscribers.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"connected\":true}"));
            for (DfsEvent event : recent) {
                if (visibleTo(event, uid)) {
                    emitter.send(SseEmitter.event().name(event.getType().name()).data(event));
                }
            }
        } catch (IOException e) {
            subscribers.remove(emitter);
        }
        return emitter;
    }

    public List<DfsEvent> recentEvents(String userId) {
        String uid = userId == null ? "" : userId;
        return recent.stream().filter(e -> visibleTo(e, uid)).collect(Collectors.toList());
    }

    private boolean visibleTo(DfsEvent event, String uid) {
        return event.getUserId() == null || event.getUserId().equals(uid);
    }

    public void publish(DfsEvent event) {
        recent.addLast(event);
        while (recent.size() > MAX_RECENT) {
            recent.pollFirst();
        }
        subscribers.forEach((emitter, uid) -> {
            if (!visibleTo(event, uid)) {
                return;
            }
            try {
                emitter.send(SseEmitter.event().name(event.getType().name()).data(event));
            } catch (Exception e) {
                subscribers.remove(emitter);
                try {
                    emitter.complete();
                } catch (Exception ignore) {
                    // already closed
                }
            }
        });
    }

    // ---- file events (scoped to their owner) ----

    public void uploadStarted(String fileName, long size, String userId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.UPLOAD_STARTED)
                .userId(userId).fileName(fileName).size(size)
                .message("Upload started: " + fileName).build());
    }

    public void encrypted(String fileName, long encryptedSize, String userId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.ENCRYPTED)
                .userId(userId).fileName(fileName).size(encryptedSize)
                .message("Encrypted " + fileName + " (AES-256)").build());
    }

    public void replicaStored(String fileName, String nodeId, int replicaIndex, int replicationFactor, String userId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.REPLICA_STORED)
                .userId(userId).fileName(fileName).nodeId(nodeId)
                .replicaIndex(replicaIndex).replicationFactor(replicationFactor)
                .message("Stored replica " + replicaIndex + "/" + replicationFactor + " of " + fileName + " on " + nodeId).build());
    }

    public void uploadCommitted(String fileName, int replicas, int replicationFactor, String userId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.UPLOAD_COMMITTED)
                .userId(userId).fileName(fileName).replicas(replicas).replicationFactor(replicationFactor)
                .message("Committed " + fileName + " — " + replicas + "/" + replicationFactor + " replicas").build());
    }

    public void replicaRestored(String fileName, String targetNodeId, String sourceNodeId, String userId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.REPLICA_RESTORED)
                .userId(userId).fileName(fileName).nodeId(targetNodeId).sourceNodeId(sourceNodeId)
                .message("Restored replica of " + fileName + " onto " + targetNodeId + " (from " + sourceNodeId + ")").build());
    }

    public void fileDeleted(String fileName, String userId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.FILE_DELETED)
                .userId(userId).fileName(fileName).message("Deleted " + fileName + " from all nodes").build());
    }

    // ---- cluster events (global, userId == null) ----

    public void nodeKilled(String nodeId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.NODE_KILLED)
                .nodeId(nodeId).message("Node " + nodeId + " went down").build());
    }

    public void nodeRevived(String nodeId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.NODE_REVIVED)
                .nodeId(nodeId).message("Node " + nodeId + " rejoined the cluster").build());
    }

    public void healStarted(String nodeId, int affectedFiles) {
        publish(DfsEvent.builder().type(DfsEvent.Type.HEAL_STARTED)
                .nodeId(nodeId).replicas(affectedFiles)
                .message("Re-replicating " + affectedFiles + " file(s) after " + nodeId + " failed").build());
    }

    public void healCompleted(String nodeId) {
        publish(DfsEvent.builder().type(DfsEvent.Type.HEAL_COMPLETED)
                .nodeId(nodeId).message("Self-heal complete after " + nodeId + " failure").build());
    }
}
