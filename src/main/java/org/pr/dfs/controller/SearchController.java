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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Tag(name = "Search Operations" , description = "APIs for searching files and metadata")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/files")
    @Operation(summary = "Search files", description = "Search files using various criteria")
    public ResponseEntity<ApiResponse<SearchResult>> searchFiles(@ModelAttribute SearchRequest request) {
        try {
            User currentUser = validateUser();
            log.info("User {} searching files with query: {}", currentUser.getUsername(), request.getQuery());

            SearchResult result = searchService.searchFiles(request);

            return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        }
        catch(Exception e) {
            log.error("Error searching files", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Search failed: " + e.getMessage()));
        }
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions", description = "Get file name suggestions for autocomplete")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(@Parameter(description = "Search query for suggestions") @RequestParam String query) {
        try {
            User currentUser = validateUser();
            log.debug("User {} getting suggestions for: {}", currentUser.getUsername(), query);

            List<String> suggestions = searchService.getFileNameSuggestions(currentUser.getUsername(),query);

            return ResponseEntity.ok(ApiResponse.success("Suggestions retrieved successfully", suggestions));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error getting search suggestions", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get suggestions: " + e.getMessage()));
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
