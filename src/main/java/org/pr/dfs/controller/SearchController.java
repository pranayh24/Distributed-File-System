package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.dto.SearchRequest;
import org.pr.dfs.dto.SearchResult;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.service.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search Operations", description = "APIs for searching files and metadata")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/files")
    @Operation(summary = "Search files", description = "Search files using various criteria")
    public ResponseEntity<ApiResponse<SearchResult>> searchFiles(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String contentType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "uploadTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {
        try {
            User currentUser = validateUser();
            log.info("User {} searching files with query: '{}', fileName: '{}', contentType: '{}'", 
                    currentUser.getUsername(), query, fileName, contentType);

            SearchRequest request = new SearchRequest();
            request.setQuery(query);
            request.setFileName(fileName);
            request.setContentType(contentType);
            request.setPage(page);
            request.setSize(size);
            request.setSortBy(sortBy);
            request.setSortDirection(sortDirection);

            SearchResult result = searchService.searchFiles(request);
            
            log.info("Search completed for user {}: {} results found", 
                    currentUser.getUsername(), result.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
        } catch (IllegalStateException e) {
            log.error("Authentication error during search: {}", e.getMessage());
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error searching files for query '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get file name suggestions for autocomplete")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
            @Parameter(description = "Search query for suggestions") @RequestParam String query) {
        try {
            User currentUser = validateUser();
            log.debug("User {} getting suggestions for: '{}'", currentUser.getUsername(), query);

            List<String> suggestions = searchService.getFileNameSuggestions(currentUser.getUserId(), query);

            return ResponseEntity.ok(ApiResponse.success("Suggestions retrieved successfully", suggestions));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error getting search suggestions for '{}': {}", query, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get suggestions: " + e.getMessage()));
        }
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent files", description = "Get recent accessed files")
    public ResponseEntity<ApiResponse<SearchResult>> getRecentFiles(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = validateUser();
            log.info("User {} getting recent files (page: {}, size: {})", 
                    currentUser.getUsername(), page, size);

            SearchResult result = searchService.getRecentFiles(currentUser.getUserId(), page, size);

            return ResponseEntity.ok(ApiResponse.success("Recent files retrieved successfully", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error getting recent files: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get recent files: " + e.getMessage()));
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "Get popular files", description = "Get most accessed files")
    public ResponseEntity<ApiResponse<SearchResult>> getPopularFiles(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = validateUser();
            log.info("User {} getting popular files (page: {}, size: {})", 
                    currentUser.getUsername(), page, size);

            SearchResult result = searchService.getPopularFiles(currentUser.getUserId(), page, size);

            return ResponseEntity.ok(ApiResponse.success("Popular files retrieved successfully", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error getting popular files: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get popular files: " + e.getMessage()));
        }
    }

    @GetMapping("/tags/{tag}")
    @Operation(summary = "Search by tag", description = "Search files by specific tag")
    public ResponseEntity<ApiResponse<SearchResult>> searchByTag(
            @Parameter(description = "Tag to search for") @PathVariable String tag,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = validateUser();
            log.info("User {} searching files by tag: '{}' (page: {}, size: {})", 
                    currentUser.getUsername(), tag, page, size);

            SearchResult result = searchService.searchByTag(currentUser.getUserId(), tag, page, size);

            return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error searching files by tag '{}': {}", tag, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tag search failed: " + e.getMessage()));
        }
    }

    private User validateUser() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return currentUser;
    }
}