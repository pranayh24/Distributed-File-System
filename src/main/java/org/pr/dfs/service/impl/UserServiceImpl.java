package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.config.DfsConfig;
import org.pr.dfs.model.User;
import org.pr.dfs.repository.UserRepository;
import org.pr.dfs.service.EncryptionService;
import org.pr.dfs.service.UserService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final DfsConfig dfsConfig;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;

    @Override
    public User createUser(String username, String email, String password) throws Exception {
        User existingUser = userRepository.findByUsername(username);
        if(existingUser != null) {
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
        user.setQuotaLimit(10L * 1024 * 1024); // 10 MB for now
        user.setCurrentUsage(0L);

        user = userRepository.save(user);

        try {
            SecretKey userKey = encryptionService.generateUserKey(userId, password);
            log.info("Generated encryption key for user: {}", username);
        } catch (Exception e) {
            log.error("Failed to generate encryption key for user: {}", username);

            userRepository.delete(user);
            throw new RuntimeException("Failed to create encryption key for user: " + username, e);
        }

        createUserDirectory(userId);

        log.info("Created new User: {} with directory: {}", username, userDirectory);

        return user;
    }

    @Override
    public User getUserById(String userId) throws Exception {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    @Override
    public User getUserByUsername(String username) throws Exception {
        User user = userRepository.findByUsername(username);
        if(user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        return user;
    }

    @Override
    public boolean validateUser(String username, String password) throws Exception {
        User user = userRepository.findByUsername(username);
        if(user == null || !user.isActive()) {
            return false;
        }

        String hashedPassword = hashPassword(password);
        boolean passwordValid = hashedPassword.equals(user.getPasswordHash());

        if(passwordValid) {
            try {
                SecretKey userKey = encryptionService.getUserKey(user.getUserId());
                if(userKey == null) {
                    log.warn("User {} has no encryption key, regenerating...", username);

                    encryptionService.generateUserKey(user.getUserId(), password);
                }
            } catch (Exception e) {
                log.error("Failed to validate encryption key for user: {}", username, e.getMessage());
                // for now let the user login
            }
        }
        return passwordValid;
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
    public void updateUserStorageUsage(String userId, long sizeChange) throws Exception {
        User user = getUserById(userId);
        long newUsage = user.getCurrentUsage() + sizeChange;

        if (newUsage < 0) {
            newUsage = 0;
        }

        user.setCurrentUsage(newUsage);
        userRepository.save(user);

        log.debug("Updated storage usage for user {}: {} -> {} (change: {})",
                user.getUsername(), user.getCurrentUsage() - sizeChange, newUsage, sizeChange);
    }

    @Override
    public void updateLastLoginTime(String userId) throws Exception {
        User user = getUserById(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.debug("Updated last login time for user: {}", user.getUsername());
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        User user = getUserById(userId);

        String oldPasswordHash = hashPassword(oldPassword);
        if(!oldPasswordHash.equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        SecretKey oldKey = encryptionService.getUserKey(userId);
        if(oldKey == null) {
            throw new IllegalArgumentException("No encryption key found for user");
        }

        String newPasswordHash = hashPassword(newPassword);

        SecretKey newKey = encryptionService.generateUserKey(user.getUserId(), newPassword);

        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        log.info("Password and encryption key updated for user: {}", user.getUsername());

        // todo : re-encrypt all user files with the new key
        log.warn("Password changed for user {}. Existing files may need re-encryption.", user.getUsername());
    }

    @Override
    public boolean hasValidEncryptionKey(String userId) {
        try {
            SecretKey key = encryptionService.getUserKey(userId);
            return key != null;
        } catch (Exception e) {
            log.warn("Failed to check encryption key for user {}: {}", userId, e.getMessage());
            return false;
        }
    }
}
