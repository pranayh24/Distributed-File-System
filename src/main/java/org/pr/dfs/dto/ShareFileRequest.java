package org.pr.dfs.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShareFileRequest {

    @NotBlank(message = "File path is required")
    private String filePath;

    private String password; // optional

    @Min(value = 1, message = "Share limit must be at least 1")
    @Max(value = 100, message = "Share limit cannot exceed 100")
    private int shareLimit = 5;

    private String description;
}
