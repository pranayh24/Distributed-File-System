package org.pr.dfs.service;

import javax.crypto.SecretKey;

public interface EncryptionService {

    byte[] encryptFile(byte[] fileData, String userId) throws Exception;
    byte[] decryptFile(byte[] encryptedData, String userId) throws Exception;

    SecretKey generateUserKey(String userId, String password) throws Exception;
    SecretKey getUserKey(String userId) throws Exception;
}
