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
import org.pr.dfs.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Target directory") @RequestParam(value="targetDirectory", defaultValue="/") String targetDirectory,
            @Parameter(description = "Replication Factor") @RequestParam(value ="replicationFactor", defaultValue = "3") int replicationFactor,
            @Parameter(description = "Version comment") @RequestParam(value="comment", required=false) String comment,
            @Parameter(description = "createVersion") @RequestParam(value="createVersion", defaultValue = "false") boolean createVersion) {

        try {
            log.info("Uploading file: {} to directory: {}", file.getOriginalFilename(), targetDirectory);

            FileUploadRequest request = new FileUploadRequest();
            request.setFile(file);
            request.setTargetDirectory(targetDirectory);
            request.setReplicationFactor(replicationFactor);
            request.setComment(comment);
            request.setCreateVersion(createVersion);

            FileMetaDataDto result = fileService.uploadFile(request);

            log.info("File uploaded successfully: {}", result.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("File uploaded successfully", result));
        } catch(Exception e) {
            log.error("Error uploading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/download/**")
    @Operation(summary = "Download a file", description = "Download a file from the distributed file system")
    public ResponseEntity<Resource> downloadFile(
            HttpServletRequest request,
            @Parameter(description = "File path") @PathVariable String filePath) {

        try {
            String fullPath = request.getRequestURI().substring("/api/files/download/".length());

            log.info("Downloading file: {}", fullPath);

            Resource resource = fileService.downloadFile(fullPath);

            String contentType = "application/octet-stream";
            try {
                contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            } catch (Exception e) {
                log.debug("Could not determine file type for: {}", fullPath);
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch(Exception e) {
            log.error("Error downloading file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/info/**")
    @Operation(summary = "Get file metadata", description = "Get metadata information about a file")
    public ResponseEntity<ApiResponse<FileMetaDataDto>> getFileInfo(HttpServletRequest request) {
        try {
            String filePath = request.getRequestURI().substring("/api/files/info/".length());

            log.info("Getting file info for: {}", filePath);

            FileMetaDataDto metaData = fileService.getFileMetaData(filePath);

            return ResponseEntity.ok(ApiResponse.success(metaData));
        } catch (Exception e) {
            log.error("Error getting file info: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("File not found " + e.getMessage()));
        }
    }

    @DeleteMapping("/**")
    @Operation(summary = "Delete a file", description = "Delete a file from the distribution file system")
    public ResponseEntity<ApiResponse<Void>> deleteFile(HttpServletRequest request) {
        try {
            String filePath = request.getRequestURI().substring("/api/files/".length());

            log.info("Deleting file: {}", filePath);

            fileService.deleteFile(filePath);

            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
        } catch(Exception e) {
            log.error("Error deleting file: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete file: " + e.getMessage()));
        }
    }
}
