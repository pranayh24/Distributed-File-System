package org.pr.dfs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccessSharedFileRequest {

    @NotBlank(message = "Share key is required")
    private String shareKey;

    private String password; // PIN if required
}
