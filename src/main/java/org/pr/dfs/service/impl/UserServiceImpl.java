package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.model.User;
import org.pr.dfs.service.UserService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DfsConfig dfsConfig;

    private final Map<String, User> userStore = new ConcurrentHashMap<>();
    private final Map<String, User> usernameIndex = new ConcurrentHashMap<>();

    @Override
    public User createUser(String username, String email, String password) throws Exception {
        if(usernameIndex.containsKey(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        String userId = UUID.randomUUID().toString();
        String passwordHash = hashPassword(password);
        String userDirectory = "users/" + username.toLowerCase().replaceAll("[^a-z0-9_]", "_");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setUserId(userId);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(LocalDateTime.now());
        user.setActive(true);
        user.setUserDirectory(userDirectory);
        user.setQuotaLimit(10L * 1024 * 1024); // 1 GB as for now
        user.setCurrentUsage(0L);

        userStore.put(userId, user);
        usernameIndex.put(username, user);

        createUserDirectory(userId);

        log.info("Created new User: {} with directory: {}", username, userDirectory);

        return user;
    }

    @Override
    public User getUserById(String userId) throws Exception {
        User user = userStore.get(userId);
        if(user == null) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        return user;
    }

    @Override
    public User getUserByUsername(String username) throws Exception {
        User user = usernameIndex.get(username);
        if(user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        return user;
    }

    @Override
    public boolean validateUser(String username, String password) throws Exception {
        User user = getUserByUsername(username);
        if(user == null || !user.isActive()) {
            return false;
        }

        String hashedPassword = hashPassword(password);
        return hashedPassword.equals(user.getPasswordHash());
    }

    @Override
    public void createUserDirectory(String userId) throws Exception {
        User user = getUserById(userId);
        Path userDirPath = Paths.get(dfsConfig.getStorage().getPath(), user.getUserDirectory());

        try {
            Files.createDirectories(userDirPath);
            log.info("Created directory for user {}: {}", user.getUsername(), userDirPath);
        } catch (IOException e) {
            log.error("Failed to create directory for user {}: {}", user.getUsername(), e.getMessage());
            throw new RuntimeException("Failed to create directory for user " + user.getUsername(), e);
        }
    }

    @Override
    public long getUserStorageUsage(String userId) throws Exception {
        User user = getUserById(userId);
        return user.getCurrentUsage();
    }

    @Override
    public void updateUserStorageUsage(String userId, long bytes) throws Exception {
        User user = getUserById(userId);
        user.setCurrentUsage(user.getCurrentUsage() + bytes);

        if (user.getCurrentUsage() > user.getQuotaLimit()) {
            log.warn("User {} exceeded quota: {} / {}", user.getUsername(),
                    user.getCurrentUsage(), user.getQuotaLimit());
        }
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }
}
