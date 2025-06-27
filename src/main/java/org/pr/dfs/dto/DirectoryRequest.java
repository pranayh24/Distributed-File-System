package org.pr.dfs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DirectoryRequest {
    @NotBlank(message = "Directory path is required")
    @Pattern(regexp = "^[a-zA-Z0-9/_.-]+$", message= "Invalid directory path")
    private String path;
}
