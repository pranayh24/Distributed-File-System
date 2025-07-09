package org.pr.dfs.repository;

import org.pr.dfs.dto.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    List<FileMetadata> findByUserIdAndIsDeletedFalse(String userId);

    Optional<FileMetadata> findByFilePathAndIsDeletedFalse(String filePath);

    @Query("SELECT f FROM FileMetadata f WHERE f.userId = :userId AND f.isDeleted = false AND " +
            "(LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<FileMetadata> searchByTextQuery(@Param("userId") String userId, @Param("query") String query,
                                            Pageable pageable);
}
