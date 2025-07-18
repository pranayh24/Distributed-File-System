package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.security.SessionManager;
import org.pr.dfs.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Tag(name = "User management", description = "APIs for user management and security")
public class UserController {

    private final UserService userService;
    private final SessionManager sessionManager;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> registerUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");

            User user = userService.createUser(username, email, password);
            return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> loginUser(
            @RequestBody Map<String, String> request,
            HttpServletResponse response) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            if (userService.validateUser(username, password)) {
                User user = userService.getUserByUsername(username);

                String sessionId = sessionManager.createSession(user);

                Cookie sessionCookie = new Cookie("SESSION-ID", sessionId);
                sessionCookie.setHttpOnly(true);
                sessionCookie.setPath("/");
                sessionCookie.setMaxAge(30 * 60); // 30 minutes
                response.addCookie(sessionCookie);

                Map<String, Object> result = new HashMap<>();
                result.put("user", user);
                result.put("sessionId", sessionId);
                result.put("message", "Login successful");

                log.info("User {} logged in with session {}", username, sessionId);

                return ResponseEntity.ok(ApiResponse.success("Login successful", result));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid credentials"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }

    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(
            @CookieValue(value = "SESSION-ID", required = false) String sessionId) {

        if (sessionId == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("No session found"));
        }

        User user = sessionManager.getUserBySession(sessionId);
        if (user != null) {
            UserContext.setCurrentUser(user);
            log.info("Current user retrieved: {}", user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(user));
        } else {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Invalid or expired session"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(value = "SESSION-ID", required = false) String sessionId,
            HttpServletResponse response) {

        if (sessionId != null) {
            sessionManager.invalidateSession(sessionId);
            log.info("Session invalidated: {}", sessionId);
        }

        Cookie sessionCookie = new Cookie("SESSION-ID", "");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(0);
        response.addCookie(sessionCookie);

        UserContext.clear();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Get current user's profile information")
    public ResponseEntity<ApiResponse<User>> getUserProfile() {
        try {
            User currentUser = validateUser();
            log.info("User {} retrieving profile", currentUser.getUsername());

            return ResponseEntity.ok(ApiResponse.success(currentUser));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error retrieving user profile", e);
            return  ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve profile" + e.getMessage()));
        }
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change user password and regenerate encryption key")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody Map<String, String> request) {
        try {
            User currentUser = validateUser();
            String oldPassword =  request.get("oldPassword");
            String newPassword =  request.get("newPassword");

            if(oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Old Password and New Password are required"));
            }

            if(newPassword.length() < 8) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("New Password must be at least 8 characters long"));
            }

            log.info("User {} changing password", currentUser.getUsername());

            userService.changePassword(currentUser.getUserId(), oldPassword, newPassword);

            return ResponseEntity.ok(ApiResponse.success("Password changed successfully. Please log in again."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).
                    body(ApiResponse.error("Authentication required"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid password: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error changing password", e);
            return  ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to change password: " + e.getMessage()));
        }
    }

    private User validateUser() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return currentUser;
    }
}
