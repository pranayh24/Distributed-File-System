package org.pr.dfs.dto;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "file_metadata")
public class FileMetadata {

    @Id
    private String fileId;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String userId;

    private String contentType;

    @Column(nullable = false)
    private LocalDateTime uploadTime;

    private LocalDateTime lastModified;

    private LocalDateTime lastAccessed;

    private String checksum;

    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "file_tags", joinColumns = @JoinColumn(name = "file_id"))
    @Column(name = "tag")
    private Set<String> tags;

    private Integer replicationFactor;

    private Integer currentReplicas;

    @Builder.Default
    private Long accessCount = 0L;

    private Boolean isDeleted = false;
}
