package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.dto.DirectoryRequest;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.dto.MoveRequest;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.service.DirectoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/directories")
@RequiredArgsConstructor
@Tag(name = "Directory Operations", description = "APIs for directory management")
public class DirectoryController {

    private final DirectoryService directoryService;

    @GetMapping("/")
    public ResponseEntity<ApiResponse<List<FileMetaDataDto>>> listRootDirectory() {
        try {
            User currentUser = validateUser();
            log.info("User {} listing root directory", currentUser.getUsername());

            List<FileMetaDataDto> files = directoryService.listDirectory("/");
            return ResponseEntity.ok(ApiResponse.success("Directory listed successfully", files));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error listing root directory", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to list directory: " + e.getMessage()));
        }
    }

    @GetMapping("/{path}")
    @Operation(summary = "List directory content", description = "Get the contents of a directory")
    public ResponseEntity<ApiResponse<List<FileMetaDataDto>>> listDirectory(@PathVariable String path) {
        try {
            User currentUser = validateUser();
            log.info("User {} listing directory: {}", currentUser.getUsername(), path);

            List<FileMetaDataDto> files = directoryService.listDirectory(path);
            return ResponseEntity.ok(ApiResponse.success("Directory listed successfully", files));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error listing directory: {}", path, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to list directory: " + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Create directory", description = "Create a new directory")
    public ResponseEntity<ApiResponse<String>> createDirectory(@Valid @RequestBody DirectoryRequest request) {
        try {
            User currentUser = validateUser();
            log.info("User {} creating directory: {}", currentUser.getUsername(), request.getPath());

            boolean created = directoryService.createDirectory(request.getPath());
            if (created) {
                return ResponseEntity.ok(ApiResponse.success("Directory created successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to create directory"));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error creating directory: {}", request.getPath(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create directory: " + e.getMessage()));
        }
    }


    @DeleteMapping("/{path}")
    @Operation(summary = "Delete directory", description = "Delete a directory and its contents")
    public ResponseEntity<ApiResponse<String>> deleteDirectory(@PathVariable String path) {
        try {
            User currentUser = validateUser();
            log.info("User {} deleting directory: {}", currentUser.getUsername(), path);

            boolean deleted = directoryService.deleteDirectory(path);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Directory deleted successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Directory not found or could not be deleted"));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error deleting directory: {}", path, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete directory: " + e.getMessage()));
        }
    }

    @PutMapping("/move")
    @Operation(summary = "Move/rename file or directory", description = "Move or rename a file or directory")
    public ResponseEntity<ApiResponse<String>> moveOrRename(@RequestBody MoveRequest request) {
        try {
            User currentUser = validateUser();
            log.info("User {} moving directory from {} to {}",
                    currentUser.getUsername(), request.getSourcePath(), request.getDestinationPath());

            boolean moved = directoryService.moveOrRename(request.getSourcePath(), request.getDestinationPath());
            if (moved) {
                return ResponseEntity.ok(ApiResponse.success("Directory moved successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to move directory"));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error moving directory from {} to {}",
                    request.getSourcePath(), request.getDestinationPath(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to move directory: " + e.getMessage()));
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
