package org.pr.dfs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileMetaDataDto {
    private String name;
    private String path;
    private boolean isDirectory;
    private long size;
    private LocalDateTime uploadTime;
    private LocalDateTime lastModified;
    private String checksum;
    private int replicationFactor;
    private int currentReplicas;
    private String contentType;
}
