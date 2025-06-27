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

    @GetMapping("/**")
    @Operation(summary = "List directory content", description = "Get the contents of a directory")
    public ResponseEntity<ApiResponse<List<FileMetaDataDto>>> listDirectory(HttpServletRequest request) {
        try {
            String directoryPath = request.getRequestURI().substring("/api/directories/".length());
            if(directoryPath.isEmpty()) {
                directoryPath= "/";
            }

            log.info("Listing directory: {}", directoryPath);

            List<FileMetaDataDto> contents = directoryService.listDirectory(directoryPath);

            return ResponseEntity.ok(ApiResponse.success(contents));
        } catch (Exception e) {
            log.error("Error listing directory: {}",e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to list directory", e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Create directory", description = "Create a new directory")
    public ResponseEntity<ApiResponse<Void>> createDirectory(@Valid @RequestBody DirectoryRequest request) {
        try {
            log.info("Creating directory: {}", request.getPath());

            directoryService.createDirectory(request.getPath());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Directory created succesfully", null));
        } catch(Exception e) {
            log.error("Error creating directory: {}",e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create directory", e.getMessage()));
        }
    }

    @DeleteMapping("/**")
    @Operation(summary = "Delete directory", description = "Delete a directory and its contents")
    public ResponseEntity<ApiResponse<Void>> deleteDirectory(HttpServletRequest request) {
        try {
            String directoryPath = request.getRequestURI().substring("/api/directories/".length());

            log.info("Deleting directory: {}", directoryPath);

            directoryService.deleteDirectory(directoryPath);

            return ResponseEntity.ok(ApiResponse.success("Directory deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting directory: {}",e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete directory", e.getMessage()));
        }
    }

    @PutMapping("/move")
    @Operation(summary = "Move/rename file or directory", description = "Move or rename a file or directory")
    public ResponseEntity<ApiResponse<Void>> moveOrRename(@RequestBody MoveRequest request) {
        try {
            log.info("Moving {} to {}", request.getSourcePath(), request.getDestinationPath());

            directoryService.moveOrRename(request.getSourcePath(), request.getDestinationPath());

            return ResponseEntity.ok(ApiResponse.success("File/directory moved successfully", null));
        } catch (Exception e) {
            log.error("Error moving file/directory: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to move file/directory", e.getMessage()));
        }
    }
}
