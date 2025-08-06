package org.pr.dfs.repository;

import org.pr.dfs.model.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {

    Share findByShareKey(String shareKey);

    List<Share> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Share> findByUserIdAndActiveTrue(String userId);

    List<Share> findByExpiresAtBeforeAndActiveTrue(LocalDateTime dateTime);

    long countByUserIdAndActiveTrue(String userId);
}