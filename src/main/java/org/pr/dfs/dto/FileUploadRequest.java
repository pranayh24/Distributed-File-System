package org.pr.dfs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class FileUploadRequest {
    @NotNull(message="File is required")
    private MultipartFile file;

    private String targetDirectory = "/";

    @Min(value=1, message="Replication factor must be at least 1")
    @Max(value=10, message="Replication factor cannot exceed 10")
    private int replicationFactor = 3;

    private String comment;
    private boolean createVersion = false;
}
