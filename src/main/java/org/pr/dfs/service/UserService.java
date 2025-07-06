package org.pr.dfs.service;

import org.pr.dfs.model.User;

public interface UserService {
    User createUser(String username, String email, String password) throws Exception;
    User getUserById(String userId) throws Exception;
    User getUserByUsername(String username) throws Exception;
    boolean validateUser(String username, String password) throws Exception;
    void createUserDirectory(String userId) throws Exception;
    long getUserStorageUsage(String userId) throws Exception;
    void updateUserStorageUsage(String userId, long bytes) throws Exception;
    void updateLastLoginTime(String userId) throws Exception;
}
