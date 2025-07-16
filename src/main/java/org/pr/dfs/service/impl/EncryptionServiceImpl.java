package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.model.UserEncryptionKey;
import org.pr.dfs.repository.UserEncryptionRepository;
import org.pr.dfs.service.EncryptionService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService {

    private final UserEncryptionRepository keyRepository;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 100000;
    private static final int SALT_LENGTH = 32;

    @Override
    public byte[] encryptFile(byte[] fileData, String userId) throws Exception {
        SecretKey userKey = getUserKey(userId);
        if(userKey == null){
            throw new IllegalStateException("No encryption key found for userId: " + userId);
        }

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, userKey, gcmSpec);

        byte[] encryptedData = cipher.doFinal(fileData);

        byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.length);

        log.debug("File encrypted for user: {} (size: {} -> {}", userId, fileData.length, result.length);
        return result;
    }

    @Override
    public byte[] decryptFile(byte[] encryptedData, String userId) throws Exception {
        SecretKey userKey = getUserKey(userId);
        if(userKey == null){
            throw new IllegalStateException("No encryption key found for userId: " + userId);
        }

        if(encryptedData.length <  GCM_IV_LENGTH){
            throw new IllegalArgumentException("Invalid encrypted data format");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

        byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, userKey, gcmSpec);

        byte[] decryptedData = cipher.doFinal(cipherText);

        log.debug("File decrypted for user: {} (size: {} -> {}", userId, encryptedData.length, decryptedData.length);

        return decryptedData;
    }

    @Override
    public SecretKey generateUserKey(String userId, String password) throws Exception {
        String salt = generateSalt();
        SecretKey key = deriveKeyFromPassword(password, salt);

        storeUserKey(userId, key, salt);

        log.info("Generated new encryption key for user: {}", userId);
        return key;
    }

    @Override
    public SecretKey getUserKey(String userId) throws Exception {
        UserEncryptionKey keyEntity = keyRepository.findByUserId(userId);
        if(keyEntity == null){
            return  null;
        }

        byte[] keyBytes = Base64.getDecoder().decode(keyEntity.getEncryptedKey());
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }

    @Override
    public void storeUserKey(String userId, SecretKey key) throws Exception{
        storeUserKey(userId, key, generateSalt());
    }

    private void storeUserKey(String userId, SecretKey key, String salt) throws Exception {
        String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());

        UserEncryptionKey keyEntity = keyRepository.findByUserId(userId);
        if(keyEntity == null){
            keyEntity = new UserEncryptionKey();
            keyEntity.setUserId(userId);
        }

        keyEntity.setEncryptedKey(encodedKey);
        keyEntity.setSalt(salt);
        keyEntity.setAlgorithm(ALGORITHM);
        keyEntity.setKeyLength(KEY_LENGTH);
        keyEntity.setCreatedAt(LocalDateTime.now());
        keyEntity.setLastUsed(LocalDateTime.now());

        keyRepository.save(keyEntity);
        log.debug("Stored encryption key for user: {}", userId);
    }

    @Override
    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public String encrypt(String plainText, SecretKey userKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);

        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(iv);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, userKey, gcmSpec);

        byte[] encryptedData = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] result = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, result, GCM_IV_LENGTH, encryptedData.length);

        return Base64.getEncoder().encodeToString(result);
    }

    @Override
    public String decrypt(String encryptedText, SecretKey userKey) throws Exception {
        byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

        if(encryptedData.length < GCM_IV_LENGTH){
            throw new IllegalArgumentException("Invalid encrypted data format");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);

        byte[] cipherText = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, GCM_IV_LENGTH, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, userKey, gcmSpec);

        byte[] decryptedData = cipher.doFinal(cipherText);

        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    @Override
    public String calculateFileHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public boolean verifyFileIntegrity(byte[] data, String expectedHash) {
        String actualHash = calculateFileHash(data);
        return actualHash.equals(expectedHash);
    }

    private SecretKey deriveKeyFromPassword(String password, String salt) throws Exception{
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] saltBytes = Base64.getDecoder().decode(salt);

        KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATION_COUNT, KEY_LENGTH);

        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), ALGORITHM);
    }
}
