package org.pr.dfs.repository;

import org.pr.dfs.dto.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    List<FileMetadata> findByUserIdAndIsDeletedFalse(String userId);

    Optional<FileMetadata> findByFilePathAndIsDeletedFalse(String filePath);

    @Query("SELECT f FROM FileMetadata f WHERE f.userId = :userId AND f.isDeleted = false AND " +
            "(LOWER(f.fileName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(f.description) LIKE LOWER(CONCAT('%',:query, '%')))")
    Page<FileMetadata> searchByTextQuery(@Param("userId") String userId, @Param("query") String query,
                                            Pageable pageable);

    @Query("SELECT f FROM FileMetadata f WHERE f.userId = :userId AND f.isDeleted = false " +
            "AND (:fileName IS NULL OR LOWER(f.fileName) LIKE LOWER(CONCAT('%', :fileName, '%'))) " +
            "AND (:contentType IS NULL OR f.contentType = :contentType) " +
            "AND (:minSize IS NULL OR f.fileSize >= :minSize) " +
            "AND (:maxSize IS NULL OR f.fileSize <= :maxSize) " +
            "AND (:fromDate IS NULL OR f.uploadTime >= :fromDate) " +
            "AND (:toDate IS NULL OR f.uploadTime <= :toDate)")
    Page<FileMetadata> advancedSearch(@Param("userId") String userId, @Param("fileName") String fileName,
                                      @Param("contentType") String contentType, @Param("minSize") Long minSize,
                                      @Param("maxSize") Long maxSize, @Param("fromDate")LocalDateTime fromDate,
                                      @Param("toDate") LocalDateTime toDate, Pageable pageable);

    @Query("SELECT f FROM FileMetadata f JOIN f.tags t WHERE f.userId = :userId AND f.isDeleted = false " +
            "AND LOWER(t) LIKE LOWER(CONCAT('%', :tag, '%'))")
    Page<FileMetadata> searchByTag(@Param("userId") String userId, @Param("tag") String tag,
                                   Pageable pageable);
}
