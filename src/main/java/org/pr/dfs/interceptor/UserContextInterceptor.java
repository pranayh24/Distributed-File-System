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

        log.info("=== INTERCEPTOR DEBUG ===");
        log.info("Request URI: {}", requestPath);
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Context Path: {}", request.getContextPath());
        log.info("Servlet Path: {}", request.getServletPath());

        // Allow public endpoints
        if (isPublicEndpoint(requestPath)) {
            log.info("PUBLIC endpoint, allowing access: {}", requestPath);
            return true;
        }

        // Get session ID from header or cookie
        String sessionId = getSessionId(request);
        log.info("Session ID extracted: {}", sessionId);

        User user = null;
        if (sessionId != null) {
            user = sessionManager.getUserBySession(sessionId);
            log.info("User found for session: {}", user != null ? user.getUsername() : "NULL");
        } else {
            log.warn("NO SESSION ID FOUND!");
        }

        // authentication for all file and directory operations
        boolean requiresAuth = requiresAuthentication(requestPath);
        log.info("Path {} requires authentication: {}", requestPath, requiresAuth);

        if (requiresAuth) {
            if (user == null) {
                log.error("UNAUTHORIZED: No user found for path: {}", requestPath);
                sendUnauthorizedResponse(response);
                return false;
            }

            UserContext.setCurrentUser(user);
            log.info("SUCCESS: User context set for user: {} accessing: {}", user.getUsername(), requestPath);
        }

        log.info("=== END INTERCEPTOR DEBUG ===");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private boolean isPublicEndpoint(String requestPath) {
        boolean isPublic = requestPath.contains("/user/") ||
                requestPath.contains("/system/health") ||
                requestPath.contains("/swagger") ||
                requestPath.contains("/api-docs") ||
                requestPath.contains("/v3/api-docs") ||
                requestPath.contains("/swagger-ui");

        log.info("Is public endpoint check: {} -> {}", requestPath, isPublic);
        return isPublic;
    }

    private boolean requiresAuthentication(String requestPath) {
        boolean requires = requestPath.contains("/files") ||
                requestPath.contains("/directories") ||
                requestPath.contains("/versions") ||
                requestPath.contains("/search");
        log.info("Requires auth check: {} -> {}", requestPath, requires);
        return requires;
    }

    private String getSessionId(HttpServletRequest request) {
        // Log all headers
        log.info("=== HEADERS ===");
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            log.info("Header: {} = {}", headerName, request.getHeader(headerName));
        }

        // Try header first
        String sessionId = request.getHeader("X-Session-ID");
        log.info("Session ID from X-Session-ID header: {}", sessionId);

        // Fallback to cookie
        if (sessionId == null) {
            sessionId = getSessionIdFromCookies(request);
            log.info("Session ID from cookies: {}", sessionId);
        }

        return sessionId;
    }

    private String getSessionIdFromCookies(HttpServletRequest request) {
        log.info("=== COOKIES ===");
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                log.info("Cookie: {} = {}", cookie.getName(), cookie.getValue());
                if ("SESSION-ID".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        } else {
            log.info("NO COOKIES FOUND");
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