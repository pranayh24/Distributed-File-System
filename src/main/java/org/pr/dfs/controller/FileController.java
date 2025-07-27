package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.dto.FileUploadRequest;
import org.pr.dfs.dto.MoveRequest;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "file Operations", description = "APIs for upload, download and management")
@Slf4j
public class FileController {

    private final FileService fileService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a file", description = "Upload a file to the distributed file system")
    public ResponseEntity<ApiResponse<FileMetaDataDto>> uploadFile(
            @Parameter(description = "File and related information") @ModelAttribute FileUploadRequest request) {

        try {
            User currentUser = validateUser();
            log.info("User {} uploading file: {}", currentUser.getUsername(),
                    request.getFile().getOriginalFilename());

            // Check user quota before upload
            if (currentUser.getCurrentUsage() + request.getFile().getSize() > currentUser.getQuotaLimit()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Upload would exceed your storage quota"));
            }

            FileMetaDataDto metadata = fileService.uploadFile(request);
            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", metadata));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    @GetMapping("/download/**")
    @Operation(summary = "Download a file", description = "Download a file from the distributed file system")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {

        try {
            User currentUser = validateUser();

            // Extract the full path from the request URI
            String requestURI = request.getRequestURI();
            String contextPath = request.getContextPath();

            // Remove the context path and the controller mapping
            String fullPath = requestURI;
            if (contextPath != null && !contextPath.isEmpty()) {
                fullPath = requestURI.substring(contextPath.length());
            }

            // Extract path after /files/download/
            String path = fullPath.substring("/files/download/".length());

            // URL decode the path
            path = java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);

            // Normalize the path - convert backslashes to forward slashes
            path = path.replace("\\", "/");

            // Remove user directory prefix if it exists (e.g., "users/username/" at the start)
            String userDir = "users/" + currentUser.getUsername() + "/";
            if (path.startsWith(userDir)) {
                path = path.substring(userDir.length());
            }

            log.info("User {} downloading file: {} (original URI: {}, normalized: {})",
                    currentUser.getUsername(), path, requestURI, path);

            Resource resource = fileService.downloadFile(path);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/info/{path}")
    @Operation(summary = "Get file metadata", description = "Get metadata information about a file")
    public ResponseEntity<ApiResponse<FileMetaDataDto>> getFileInfo(@PathVariable String path) {
        try {
            User currentUser = validateUser();
            log.info("User {} getting file info: {}", currentUser.getUsername(), path);

            FileMetaDataDto metadata = fileService.getFileMetaData(path);
            return ResponseEntity.ok(ApiResponse.success("File info retrieved", metadata));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error getting file info: {}", path, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get file info: " + e.getMessage()));
        }
    }


    @DeleteMapping("/{path}")
    @Operation(summary = "Delete a file", description = "Delete a file from the distribution file system")
    public ResponseEntity<ApiResponse<String>> deleteFile(@PathVariable String path) {
        try {
            User currentUser = validateUser();
            log.info("User {} deleting file: {}", currentUser.getUsername(), path);

            boolean deleted = fileService.deleteFile(path);
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File not found or could not be deleted"));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error deleting file: {}", path, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete file: " + e.getMessage()));
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
