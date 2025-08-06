package org.pr.dfs.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
public class ShareCryptoUtils {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";
    private static final String SECRET_KEY = "DFS_SHARE_SECRET_KEY"; // for now in dev
    private final ObjectMapper objectMapper;

    public ShareCryptoUtils() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SharePayload {
        @JsonProperty("userId")
        public String userId;

        @JsonProperty("filePath")
        public String filePath;

        @JsonProperty("fileName")
        public String fileName;

        @JsonProperty("expiresAt")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        public LocalDateTime expiresAt;

        @JsonProperty("usePassword")
        public boolean hasPassword;

        @JsonProperty("passwordHash")
        public String passwordHash;

        @JsonProperty("shareLimit")
        public int shareLimit;

        @JsonProperty("trackingId")
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

            log.debug("Decrypted JSON payload: {}", jsonPayload);

            SharePayload payload = objectMapper.readValue(jsonPayload, SharePayload.class);

            return payload;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid share key", e);
        }
    }

    private String safeGetString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.isEmpty() ? null : String.valueOf(list.get(0));
        }
        return String.valueOf(value);
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
