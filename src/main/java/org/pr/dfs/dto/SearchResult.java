package org.pr.dfs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResult {
    private List<FileSearchResultDto> files;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSearchResultDto {
        private String fileId;
        private String fileName;
        private String filePath;
        private long fileSize;
        private String contentType;
        private LocalDateTime uploadTime;
        private LocalDateTime lastModified;
        private LocalDateTime lastAccessed;
        private String description;
        private Set<String> tags;
        private long accessCount;
        private int replicationFactor;
        private int currentReplicas;
        private double relevanceScore;
    }
}
