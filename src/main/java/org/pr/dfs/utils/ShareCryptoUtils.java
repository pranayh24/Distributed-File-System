package org.pr.dfs.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class ShareCryptoUtils {

    private static final String SECRET_KEY = "DFS_SHARE_SECRET_KEY"; // for now in dev
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class SharePayload {
        public String userId;
        public String filePath;
        public String fileName;
        public LocalDateTime expiresAt;
        public boolean hasPassword;
        public String passwordHash;
        public int shareLimit;
        public String trackingId; // for db tracking
    }

    public String generateShareKey(String userId, String filePath, String fileName, int shareLimit,
                                   LocalDateTime expiresAt, String password) {
        try {
            SharePayload payload = new SharePayload();
            payload.userId = userId;
            payload.filePath = filePath;
            payload.fileName = fileName;
            payload.shareLimit = shareLimit;
            payload.expiresAt = expiresAt;
            payload.trackingId = generateShortId();

            if(password != null && !password.trim().isEmpty()) {
                payload.hasPassword = true;
                payload.passwordHash = hashPassword(password);
            } else  {
                payload.hasPassword = false;
            }

            String jsonPayload = objectMapper.writeValueAsString(payload);
            String encryptedPayload = encrypt(jsonPayload);

            String shareKey = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(encryptedPayload.getBytes(StandardCharsets.UTF_8));

            return shareKey;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate share key", e);
        }
    }

    public SharePayload decryptShareKey(String shareKey) {
        try {
            byte[] encryptedBytes = Base64.getUrlDecoder().decode(shareKey);
            String encryptedPayload = new String(encryptedBytes, StandardCharsets.UTF_8);
            String jsonPayload = decrypt(encryptedPayload);

            Map<String, Object> payloadMap = objectMapper.readValue(jsonPayload, Map.class);

            SharePayload payload = new SharePayload();
            payload.userId = (String) payloadMap.get("userId");
            payload.filePath = (String) payloadMap.get("filePath");
            payload.fileName = (String) payloadMap.get("fileName");
            payload.shareLimit = ((Number) payloadMap.get("shareLimit")).intValue();
            payload.expiresAt = LocalDateTime.parse((String) payloadMap.get("expiresAt"));
            payload.hasPassword = (Boolean) payloadMap.get("hasPassword");
            payload.passwordHash = (String) payloadMap.get("passwordHash");
            payload.trackingId = (String) payloadMap.get("trackingId");

            return payload;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid share key", e);
        }
    }

    public boolean isShareValid(SharePayload payload) {
        return payload.expiresAt == null || LocalDateTime.now().isBefore(payload.expiresAt);
    }

    public boolean verifyPassword(SharePayload payload, String providedPassword) {
        if(!payload.hasPassword) return true;
        if(providedPassword == null) return false;

        try {
            return hashPassword(providedPassword).equals(payload.passwordHash);
        } catch (Exception e) {
            return false;
        }
    }

    private String encrypt(String plainText) throws Exception {
        SecretKeySpec secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private String decrypt(String encryptedText) throws Exception {
        SecretKeySpec secretKey = getSecretKey();
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private SecretKeySpec getSecretKey() throws Exception {
        byte[] key = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }

    private String hashPassword(String password) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    private String generateShortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
