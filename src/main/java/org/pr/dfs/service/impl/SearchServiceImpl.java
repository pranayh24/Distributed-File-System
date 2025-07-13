package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.FileMetadata;
import org.pr.dfs.dto.SearchRequest;
import org.pr.dfs.dto.SearchResult;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.repository.FileMetadataRepository;
import org.pr.dfs.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final FileMetadataRepository fileMetadataRepository;

    @Override
    public SearchResult searchFiles(SearchRequest request) throws Exception {
        String userId = getCurrentUserId();

        Pageable pageable = createPageable(request);
        Page<FileMetadata> page;

        if(hasAdvancedSearchCriteria(request)) {
            page = fileMetadataRepository.advancedSearch(
                    userId,
                    request.getFileName(),
                    request.getContentType(),
                    request.getMinSize(),
                    request.getMaxSize(),
                    request.getFromDate(),
                    request.getToDate(),
                    pageable
            );

        } else if(request.getQuery() != null && !request.getQuery().trim().isEmpty()) {
            page = fileMetadataRepository.searchByTextQuery(userId, request.getQuery().trim(), pageable);
        } else {
            page = fileMetadataRepository.findAll(
                    (root, query, criteriaBuilder) -> {
                        return criteriaBuilder.and(
                                criteriaBuilder.equal(root.get("userId"), userId),
                                criteriaBuilder.equal(root.get("isDeleted"), false)
                        );
                    },
                    pageable
            );
        }

        return convertToSearchResult(page, request);
    }

    @Override
    public SearchResult searchByTag(String userId, String tag, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "uploadTime"));
        Page<FileMetadata> resultPage = fileMetadataRepository.searchByTag(userId, tag, pageable);

        return convertToSearchResult(resultPage, null);
    }

    @Override
    public SearchResult getRecentFiles(String userId, int page, int size) throws Exception {
        Pageable pageable = PageRequest.of(page, size);
        Page<FileMetadata> resultPage = fileMetadataRepository.findRecentFiles(userId, pageable);
        return convertToSearchResult(resultPage, null);
    }

    @Override
    public SearchResult getPopularFiles(String userId, int page, int size) throws Exception {
        return null;
    }

    @Override
    public List<String> getFileNameSuggestions(String userId, String query) throws Exception {
        return List.of();
    }

    @Override
    public void saveFileMetadata(FileMetadata fileMetadata) throws Exception {

    }

    @Override
    public void updateFileAccess(String filePath) throws Exception {

    }

    @Override
    public void deleteFileMetadata(String filePath) throws Exception {

    }

    @Override
    public FileMetadata getFileMetadataByPath(String filePath) throws Exception {
        return null;
    }

    private String getCurrentUserId() {
        return UserContext.getCurrentUserId();
    }

    private boolean hasAdvancedSearchCriteria(SearchRequest request) {
        return request.getFileName() != null ||
                request.getContentType() != null ||
                request.getMinSize() != null ||
                request.getMaxSize() != null ||
                request.getFromDate() != null ||
                request.getToDate() != null;
    }

    private Pageable createPageable(SearchRequest request) {
        Sort.Direction direction = "asc".equalsIgnoreCase((request.getSortDirection()))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, request.getSortBy());

        return PageRequest.of(
                Math.max(0, request.getPage()),
                Math.min(100, Math.max(1, request.getSize())),
                sort
        );
    }

    private SearchResult convertToSearchResult(Page<FileMetadata> page, SearchRequest request) {
        List<SearchResult.FileSearchResultDto> files = page.getContent().stream()
                .map(this::convertToFileSearchResultDto)
                .collect(Collectors.toList());

        return SearchResult.builder()
                .files(files)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .pageSize(page.getSize())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();

    }

    private SearchResult.FileSearchResultDto convertToFileSearchResultDto(FileMetadata fileMetadata) {
        return SearchResult.FileSearchResultDto.builder()
                .fileId(fileMetadata.getFileId())
                .fileName(fileMetadata.getFileName())
                .filePath(fileMetadata.getFilePath())
                .fileSize(fileMetadata.getFileSize())
                .contentType(fileMetadata.getContentType())
                .uploadTime(fileMetadata.getUploadTime())
                .lastModified(fileMetadata.getLastModified())
                .lastAccessed(fileMetadata.getLastAccessed())
                .description(fileMetadata.getDescription())
                .tags(fileMetadata.getTags())
                .accessCount(fileMetadata.getAccessCount())
                .replicationFactor(fileMetadata.getReplicationFactor())
                .currentReplicas(fileMetadata.getCurrentReplicas())
                .relevanceScore(calculateRelevanceScore(fileMetadata))
                .build();
    }

    private double calculateRelevanceScore(FileMetadata fileMetadata) {
        double baseScore = 1.0;

        if(fileMetadata.getAccessCount() > 0) {
            baseScore += Math.log(fileMetadata.getAccessCount()) * 0.1;
        }

        if(fileMetadata.getLastAccessed() != null) {
            long daysSinceAccess = java.time.Duration.between(fileMetadata.getLastAccessed(), LocalDateTime.now()).toDays();
            if(daysSinceAccess < 7) {
                baseScore += (7 - daysSinceAccess) * 0.05;
            }
        }

        return Math.min(baseScore, 5.0);
    }
}
