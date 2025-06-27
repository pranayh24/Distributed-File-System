package org.pr.dfs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MoveRequest {
    @NotBlank(message= "Source path is required")
    private String sourcePath;

    @NotBlank(message= "Destination path is requires")
    private String destinationPath;
}
