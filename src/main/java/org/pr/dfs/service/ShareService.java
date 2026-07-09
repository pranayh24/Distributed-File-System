package org.pr.dfs.service;

import org.pr.dfs.dto.AccessSharedFileRequest;
import org.pr.dfs.dto.ShareFileRequest;
import org.pr.dfs.dto.ShareFileResponse;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ShareService {
    
    ShareFileResponse createShare(ShareFileRequest request) throws Exception;

    Resource accessSharedFile(AccessSharedFileRequest request, String clientIP) throws Exception;

    ShareFileResponse getShareInfo(String shareKey) throws Exception;

    List<ShareFileResponse> getUserShares() throws Exception;

    boolean revokeShare(String shareKey) throws Exception;

    ShareFileResponse updateShare(String shareKey, ShareFileRequest request) throws Exception;

    void cleanupExpiredShares();
}