package org.pr.dfs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileResponse {
    private String shareKey;
    private String filePath;
    private String fileName;
    private long fileSize;
    private int shareLimit;
    private int accessCount;
    private boolean hasPassword;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String shareUrl;
}
