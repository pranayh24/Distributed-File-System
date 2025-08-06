package org.pr.dfs.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pr.dfs.dto.AccessSharedFileRequest;
import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.dto.ShareFileRequest;
import org.pr.dfs.dto.ShareFileResponse;
import org.pr.dfs.model.Share;
import org.pr.dfs.model.User;
import org.pr.dfs.model.UserContext;
import org.pr.dfs.repository.ShareRepository;
import org.pr.dfs.service.FileService;
import org.pr.dfs.service.ShareService;
import org.pr.dfs.service.UserService;
import org.pr.dfs.utils.ShareCryptoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    private final ShareRepository shareRepository;
    private final ShareCryptoUtils shareUtils;
    private final FileService fileService;
    private final UserService userService;

    @Value("${dfs.share.base-url:http://localhost:8080/api/share}")
    private String shareBaseUrl;

    @Override
    @Transactional
    public ShareFileResponse createShare(ShareFileRequest request) throws Exception {
        User currentUser = validateUser();

        log.info("User {} creating share for file: {}", currentUser.getUsername(), request.getFilePath());

        FileMetaDataDto fileMetadata = fileService.getFileMetaData(request.getFilePath());
        if(fileMetadata == null) {
            throw new IllegalArgumentException("File not found: " + request.getFilePath());
        }

        String userScopedPath = getUserScopedPath(currentUser, request.getFilePath());
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        String shareKey = shareUtils.generateShareKey(
                currentUser.getUserId(),
                userScopedPath,
                fileMetadata.getName(),
                request.getShareLimit(),
                expiresAt,
                request.getPassword()
        );

        asyncSaveToDatabase(shareKey, currentUser, request, fileMetadata, userScopedPath, expiresAt);

        log.info("Share created: {} for file: {}", shareKey.substring(0, 8) + "...", request.getFilePath());

        return ShareFileResponse.builder()
                .shareKey(shareKey)
                .filePath(userScopedPath)
                .fileName(fileMetadata.getName())
                .fileSize(fileMetadata.getSize())
                .shareLimit(request.getShareLimit())
                .accessCount(0)
                .hasPassword(request.getPassword() != null && !request.getPassword().trim().isEmpty())
                .expiresAt(expiresAt)
                .shareUrl(shareBaseUrl + "/" + shareKey)
                .build();
    }

    @Override
    public Resource accessSharedFile(AccessSharedFileRequest request, String clientIP) throws Exception {
        log.info("Accessing shared file from IP: {}", clientIP);

        ShareCryptoUtils.SharePayload payload = shareUtils.decryptShareKey(request.getShareKey());

        if (!shareUtils.isShareValid(payload)) {
            throw new IllegalArgumentException("Share has expired");
        }

        if (!shareUtils.verifyPassword(payload, request.getPassword())) {
            throw new IllegalArgumentException("Wrong password or password required");
        }

        asyncUpdateAccess(payload.trackingId, clientIP);

        User fileOwner = userService.getUserById(payload.userId);
        UserContext.setCurrentUser(fileOwner);

        try {
            String relativePath = extractRelativePath(payload.filePath, fileOwner);
            Resource resource = fileService.downloadFile(relativePath);

            log.info("Shared file {} accessed by IP: {}", payload.fileName, clientIP);
            return resource;
        } finally {
            UserContext.clear();
        }
    }

    @Override
    public ShareFileResponse getShareInfo(String shareKey) throws Exception {
        ShareCryptoUtils.SharePayload payload = shareUtils.decryptShareKey(shareKey);

        int accessCount = 0;
        try {
            Share dbShare = shareRepository.findByShareKey(payload.trackingId);
            if (dbShare != null) {
                accessCount = dbShare.getAccessCount();
            }
        } catch (Exception e) {
            log.warn("could not get access count from DB: {}", e.getMessage());
        }

        return ShareFileResponse.builder()
                .shareKey(shareKey)
                .filePath(payload.filePath)
                .fileName(payload.fileName)
                .shareLimit(payload.shareLimit)
                .accessCount(accessCount)
                .hasPassword(payload.hasPassword)
                .expiresAt(payload.expiresAt)
                .shareUrl(shareBaseUrl + "/" + shareKey)
                .build();
    }

    @Override
    public List<ShareFileResponse> getUserShares() throws Exception {
        User currentUser = validateUser();

        List<Share> userShares = shareRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getUserId());

        return userShares.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean revokeShare(String shareKey) throws Exception {
        User currentUser = validateUser();

        try {
            ShareCryptoUtils.SharePayload payload = shareUtils.decryptShareKey(shareKey);

            if (!payload.userId.equals(currentUser.getUserId())) {
                throw new IllegalArgumentException("Access denied");
            }

            Share dbShare = shareRepository.findByShareKey(payload.trackingId);
            if (dbShare != null) {
               dbShare.setActive(false);
               shareRepository.save(dbShare);
            }

            log.info("Share revoked by user: {}", currentUser.getUsername());
            return true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to revoke share");
        }
    }

    @Override
    @Transactional
    public ShareFileResponse updateShare(String shareKey, ShareFileRequest request) throws Exception {
        revokeShare(shareKey);
        return createShare(request);
    }

    @Override
    @Transactional
    public void cleanupExpiredShares() {
        LocalDateTime now = LocalDateTime.now();
        List<Share> expiredShares = shareRepository.findByExpiresAtBeforeAndActiveTrue(now);

        expiredShares.forEach(share -> share.setActive(false));

        if(!expiredShares.isEmpty()) {
            shareRepository.saveAll(expiredShares);
            log.info("Cleaned up {} expired shares", expiredShares.size());
        }
    }

    protected CompletableFuture<Void> asyncSaveToDatabase(String shareKey, User user,
                                                          ShareFileRequest request,
                                                          FileMetaDataDto fileMetadata,
                                                          String userScopedPath,
                                                          LocalDateTime expiresAt) {
        //UserContext.setCurrentUser(user);
        try {
            ShareCryptoUtils.SharePayload payload = shareUtils.decryptShareKey(shareKey);

            Share share = Share.builder()
                    .shareKey(payload.trackingId) // short
                    .userId(user.getUserId())
                    .fileId(UUID.randomUUID().toString())
                    .filePath(userScopedPath)
                    .passwordHash(payload.hasPassword ? payload.passwordHash : null)
                    .shareLimit(request.getShareLimit())
                    .accessCount(0)
                    .accessedFromIPs(new ArrayList<>())
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .expiresAt(expiresAt)
                    .build();

            shareRepository.save(share);
            log.debug("Share saved to DB: {}", payload.trackingId);
        } catch (Exception e) {
            log.error("Failed to save share to DB: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async
    protected CompletableFuture<Void> asyncUpdateAccess(String trackingId, String clientIP) {
        try {
            Share dbShare = shareRepository.findByShareKey(trackingId);
            if (dbShare != null && dbShare.isActive()) {
                dbShare.setAccessCount(dbShare.getAccessCount() + 1);

                if (dbShare.getAccessedFromIPs() == null) {
                    dbShare.setAccessedFromIPs(new ArrayList<>());
                }

                if(!dbShare.getAccessedFromIPs().contains(clientIP)) {
                    dbShare.getAccessedFromIPs().add(clientIP);
                }

                shareRepository.save(dbShare);
                log.debug("Updated access count for share: {}", trackingId);
            }
        } catch (Exception e) {
            log.error("Failed to update access count for share: {}", trackingId);
        }
        return CompletableFuture.completedFuture(null);
    }

    private ShareFileResponse convertToResponse(Share share) {
        return ShareFileResponse.builder()
                .shareKey(share.getShareKey())
                .filePath(share.getFilePath())
                .shareLimit(share.getShareLimit())
                .accessCount(share.getAccessCount())
                .hasPassword(share.getPasswordHash() != null)
                .createdAt(share.getCreatedAt())
                .expiresAt(share.getExpiresAt())
                .shareUrl(shareBaseUrl + "/" + share.getShareKey())
                .build();
    }

    private User validateUser() {
        User currentUser = UserContext.getCurrentUser();
        log.info("VALIDATE USER DEBUG: Current user from context: {}",
                currentUser != null ? currentUser.getUsername() : "NULL");

        if (currentUser == null) {
            log.error("CRITICAL: UserContext.getCurrentUser() returned null despite interceptor success");
            throw new IllegalStateException("No authenticated user found");
        }
        return currentUser;
    }

    private String getUserScopedPath(User user, String filePath) {
        String userDirectory = user.getUserDirectory();
        if (filePath == null || filePath.trim().isEmpty() || filePath.equals("/")) {
            return userDirectory;
        }

        String normalizedPath = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        return userDirectory + "/" + normalizedPath;
    }

    private String extractRelativePath(String fullPath, User user) {
        String userDirectory = user.getUserDirectory();
        if (fullPath.startsWith(userDirectory + "/")) {
            return fullPath.substring(userDirectory.length() + 1);
        } else if (fullPath.equals(userDirectory)) {
            return "";
        }
        return fullPath;
    }
}
