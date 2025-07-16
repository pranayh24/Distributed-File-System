package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.repository.UserEncryptionRepository;
import org.pr.dfs.service.EncryptionService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;

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

        return new byte[0];
    }

    @Override
    public SecretKey generateUserKey(String userId, String password) throws Exception {
        return null;
    }

    @Override
    public SecretKey getUserKey(String userId) throws Exception {
        return null;
    }

    @Override
    public String generateSalt() {
        return "";
    }

    @Override
    public String encrypt(String plainText, SecretKey userKey) throws Exception {
        return "";
    }

    @Override
    public String decrypt(String encryptedText, SecretKey userKey) throws Exception {
        return "";
    }

    @Override
    public String calculateFileHash(byte[] data) {
        return "";
    }

    @Override
    public boolean verifyFileIntegrity(byte[] data, String expectedHash) {
        return false;
    }
}
