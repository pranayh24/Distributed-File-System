package org.pr.dfs.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.model.User;
import org.pr.dfs.security.SessionManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final SessionManager sessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();

        // Allow public endpoints
        if (isPublicEndpoint(requestPath)) {
            return true;
        }

        // Get session ID from header or cookie
        String sessionId = getSessionId(request);
        User user = null;

        if (sessionId != null) {
            user = sessionManager.getUserBySession(sessionId);
        }

        // Require authentication for all file and directory operations
        if (requiresAuthentication(requestPath)) {
            if (user == null) {
                log.warn("Unauthorized access attempt to: {} from IP: {}", requestPath, request.getRemoteAddr());
                sendUnauthorizedResponse(response);
                return false;
            }

            UserContext.setCurrentUser(user);
            log.debug("User context set for user: {} accessing: {}", user.getUsername(), requestPath);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicEndpoint(String requestPath) {
        return requestPath.contains("/test/") ||
                requestPath.contains("/system/health") ||
                requestPath.contains("/swagger") ||
                requestPath.contains("/api-docs") ||
                requestPath.contains("/v3/api-docs") ||
                requestPath.contains("/swagger-ui");
    }

    private boolean requiresAuthentication(String requestPath) {
        return requestPath.contains("/files/") ||
                requestPath.contains("/directories/") ||
                requestPath.contains("/versions/");
    }

    private String getSessionId(HttpServletRequest request) {
        // Try header first
        String sessionId = request.getHeader("X-Session-ID");

        // Fallback to cookie
        if (sessionId == null) {
            sessionId = getSessionIdFromCookies(request);
        }

        return sessionId;
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("SESSION-ID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        try {
            response.getWriter().write("{\"success\":false,\"error\":\"Authentication required. Please log in first.\"}");
        } catch (Exception e) {
            log.error("Error writing unauthorized response", e);
        }
    }
}