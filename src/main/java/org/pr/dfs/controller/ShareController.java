package org.pr.dfs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.AccessSharedFileRequest;
import org.pr.dfs.dto.ApiResponse;
import org.pr.dfs.dto.ShareFileRequest;
import org.pr.dfs.dto.ShareFileResponse;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.service.ShareService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/share")
@RequiredArgsConstructor
@Tag(name = "File Sharing", description = "APIs for creating and accessing shared files")
public class ShareController {

    private final ShareService shareService;

    @PostMapping("/create")
    @Operation(summary = "Create a file share", description = "Create a shareable link for a file")
    public ResponseEntity<ApiResponse<ShareFileResponse>> createShare(
            @Valid @RequestBody ShareFileRequest request) {
        try {
            User currentUser = validateUser();
            log.info("User {} creating share for file: {}", currentUser.getUsername(), request.getFilePath());

            ShareFileResponse response = shareService.createShare(request);
            return ResponseEntity.ok(ApiResponse.success("Share created successfully", response));
        } catch (Exception e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{shareKey}")
    @Operation(summary = "Get share information", description = "Get information about a shared file")
    public ResponseEntity<ApiResponse<ShareFileResponse>> getShareInfo(
            @Parameter(description = "Share key") @PathVariable String shareKey) {
        try {
            log.info("Getting share info for key: {}", shareKey.substring(0, Math.min(8, shareKey.length())) + "...");

            ShareFileResponse response = shareService.getShareInfo(shareKey);
            return ResponseEntity.ok(ApiResponse.success("Share information retrieved", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid share key: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting share info", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get share info: " + e.getMessage()));
        }
    }

    @GetMapping("/my-shares")
    @Operation(summary = "Get user's shares", description = "Get all shares created by the current user")
    public ResponseEntity<ApiResponse<List<ShareFileResponse>>> getUserShares() {
        try {
            User currentUser = validateUser();
            log.info("User {} getting their shares", currentUser.getUsername());

            List<ShareFileResponse> shares = shareService.getUserShares();
            return ResponseEntity.ok(ApiResponse.success("User shares retrieved", shares));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (Exception e) {
            log.error("Error getting user shares", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get user shares: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{shareKey}")
    @Operation(summary = "Revoke share", description = "Revoke/deactivate a share")
    public ResponseEntity<ApiResponse<String>> revokeShare(
            @Parameter(description = "Share key") @PathVariable String shareKey) {
        try {
            User currentUser = validateUser();
            log.info("User {} revoking share: {}", currentUser.getUsername(),
                    shareKey.substring(0, Math.min(8, shareKey.length())) + "...");

            boolean revoked = shareService.revokeShare(shareKey);
            if (revoked) {
                return ResponseEntity.ok(ApiResponse.success("Share revoked successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to revoke share"));
            }

        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error revoking share", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to revoke share: " + e.getMessage()));
        }
    }

    @PutMapping("/{shareKey}")
    @Operation(summary = "Update share", description = "Update share settings")
    public ResponseEntity<ApiResponse<ShareFileResponse>> updateShare(
            @Parameter(description = "Share key") @PathVariable String shareKey,
            @Valid @RequestBody ShareFileRequest request) {
        try {
            User currentUser = validateUser();
            log.info("User {} updating share: {}", currentUser.getUsername(),
                    shareKey.substring(0, Math.min(8, shareKey.length())) + "...");

            ShareFileResponse response = shareService.updateShare(shareKey, request);
            return ResponseEntity.ok(ApiResponse.success("Share updated successfully", response));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication required"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating share", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update share: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{shareKey}")
    @Operation(summary = "Direct download link", description = "Direct download link for browsers (no auth required)")
    public ResponseEntity<?> directDownload(
            @Parameter(description = "Share key") @PathVariable String shareKey,
            @RequestParam(required = false) String password,
            HttpServletRequest httpRequest) {
        try {
            String clientIP = getClientIP(httpRequest);
            log.info("Direct download access from IP: {}", clientIP);

            AccessSharedFileRequest request = new AccessSharedFileRequest();
            request.setShareKey(shareKey);
            request.setPassword(password);

            Resource resource = shareService.accessSharedFile(request, clientIP);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("Error");
        } catch (Exception e) {
            log.error("Error in direct download", e);
            return ResponseEntity.internalServerError()
                    .contentType(MediaType.TEXT_HTML)
                    .body("Failed to download file. Please try again.");
        }
    }

    private User validateUser() {
        User currentUser = UserContext.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        return currentUser;
    }


    private String getClientIP(HttpServletRequest httpRequest) {
        String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = httpRequest.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        return httpRequest.getRemoteAddr();
    }
}
