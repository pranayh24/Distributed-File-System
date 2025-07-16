package org.pr.dfs.repository;

import org.pr.dfs.model.UserEncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserEncryptionRepository extends JpaRepository<UserEncryptionKey, String> {
    UserEncryptionKey findByUserId(String userId);
    UserEncryptionKey findByUserIdAndActiveTrue(String userId);
}
