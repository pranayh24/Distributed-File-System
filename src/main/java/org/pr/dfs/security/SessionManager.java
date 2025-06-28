package org.pr.dfs.security;

import org.pr.dfs.model.User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private final Map<String, User> sessions = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionExpiry = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT = 1000 * 60 * 30; // 30 minutes
    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user);
        sessionExpiry.put(sessionId, System.currentTimeMillis() + SESSION_TIMEOUT);
        return sessionId;
    }

    public User getUserBySession(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        Long expiry = sessionExpiry.get(sessionId);
        if (expiry == null || System.currentTimeMillis() > expiry) {
            invalidateSession(sessionId);
            return null;
        }
        sessionExpiry.put(sessionId, System.currentTimeMillis() + SESSION_TIMEOUT);
        return sessions.get(sessionId);
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
        sessionExpiry.remove(sessionId);
    }

    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        sessionExpiry.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                sessions.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

}
