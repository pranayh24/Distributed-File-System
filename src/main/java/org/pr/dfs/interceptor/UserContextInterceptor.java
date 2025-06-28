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
        if (requestPath.contains("/test/") || requestPath.contains("/system/health") ||
                requestPath.contains("/swagger") || requestPath.contains("/api-docs")) {
            return true;
        }

        String sessionId = request.getHeader("X-Session-ID");
        if (sessionId == null) {
            sessionId = getSessionIdFromCookies(request);
        }

        if (sessionId != null) {
            User user = sessionManager.getUserBySession(sessionId);
            if (user != null) {
                UserContext.setCurrentUser(user);
                log.debug("User context set for user: {}", user.getUsername());
                return true;
            }
        }

        if (requestPath.contains("/files/") || requestPath.contains("/directories/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            try {
                response.getWriter().write("{\"success\":false,\"error\":\"Authentication required\"}");
            } catch (Exception e) {
                log.error("Error writing response", e);
            }
            return false;
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
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
}