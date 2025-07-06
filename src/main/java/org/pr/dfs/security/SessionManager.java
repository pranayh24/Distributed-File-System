package org.pr.dfs.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.User;
import org.pr.dfs.service.UserService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionManager {

    private final UserService userService;

    private final Map<String, String> sessions = new ConcurrentHashMap<>(); // sessionId -> userId
    private final Map<String, Long> sessionExpiry = new ConcurrentHashMap<>();

    private static final long SESSION_TIMEOUT = 1000 * 60 * 30; // 30 minutes

    public String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessions.put(sessionId, user.getUserId()); // Store userId instead of User object
        sessionExpiry.put(sessionId, System.currentTimeMillis() + SESSION_TIMEOUT);

        try {
            userService.updateLastLoginTime(user.getUserId());
        } catch (Exception e) {
            log.warn("Failed to update last login time for user {}: {}", user.getUsername(), e.getMessage());
        }

        log.debug("Created session {} for user {}", sessionId, user.getUsername());
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

        // Extend session timeout
        sessionExpiry.put(sessionId, System.currentTimeMillis() + SESSION_TIMEOUT);

        // Get fresh user data from database
        String userId = sessions.get(sessionId);
        if (userId == null) {
            return null;
        }

        try {
            User user = userService.getUserById(userId);
            if (!user.isActive()) {
                log.warn("Session {} belongs to inactive user {}, invalidating", sessionId, user.getUsername());
                invalidateSession(sessionId);
                return null;
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to retrieve user for session {}: {}", sessionId, e.getMessage());
            invalidateSession(sessionId);
            return null;
        }
    }

    public void invalidateSession(String sessionId) {
        String userId = sessions.remove(sessionId);
        sessionExpiry.remove(sessionId);

        if (userId != null) {
            try {
                User user = userService.getUserById(userId);
                log.debug("Invalidated session {} for user {}", sessionId, user.getUsername());
            } catch (Exception e) {
                log.debug("Invalidated session {} for user ID {}", sessionId, userId);
            }
        }
    }

    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int cleanedCount = 0;

        sessionExpiry.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                sessions.remove(entry.getKey());
                return true;
            }
            return false;
        });

        if (cleanedCount > 0) {
            log.debug("Cleaned up {} expired sessions", cleanedCount);
        }
    }
}
