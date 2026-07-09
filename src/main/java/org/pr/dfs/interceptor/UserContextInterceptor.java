package org.pr.dfs.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.security.JwtService;
import org.pr.dfs.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates requests from a stateless JWT and populates {@link UserContext}.
 * The token is read from the {@code Authorization: Bearer <token>} header, or — for
 * browser {@code EventSource} which cannot set headers — from a {@code ?token=} query param.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Let CORS preflight through untouched
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();

        User user = resolveUser(request);
        if (user != null) {
            UserContext.setCurrentUser(user);
        }

        // Public endpoints (and /user/me, which returns its own 401 when no user is present)
        if (path.contains("/user/me") || isPublicEndpoint(path)) {
            return true;
        }

        if (requiresAuthentication(path) && user == null) {
            sendUnauthorized(response);
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private User resolveUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return null;
        }
        String userId = jwtService.getUserId(token);
        if (userId == null) {
            return null;
        }
        try {
            User user = userService.getUserById(userId);
            return (user != null && user.isActive()) ? user : null;
        } catch (Exception e) {
            log.debug("Could not load user {} from token: {}", userId, e.getMessage());
            return null;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        // Fallback for SSE / EventSource, which cannot send custom headers
        return request.getParameter("token");
    }

    private boolean isPublicEndpoint(String path) {
        return path.contains("/user/login")
                || path.contains("/user/register")
                || path.contains("/system/health")
                || path.contains("/swagger")
                || path.contains("/api-docs")
                || path.contains("/v3/api-docs")
                || path.contains("/swagger-ui")
                || path.contains("/share/download"); // accessing a shared file is public (by link)
    }

    private boolean requiresAuthentication(String path) {
        if (path.contains("/share/download")) {
            return false;
        }
        return path.contains("/files")
                || path.contains("/directories")
                || path.contains("/versions")
                || path.contains("/search")
                || path.contains("/share/"); // creating/listing/revoking shares needs auth
    }

    private void sendUnauthorized(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        try {
            response.getWriter().write("{\"success\":false,\"error\":\"Authentication required. Please log in.\"}");
        } catch (Exception e) {
            log.error("Error writing unauthorized response", e);
        }
    }
}
