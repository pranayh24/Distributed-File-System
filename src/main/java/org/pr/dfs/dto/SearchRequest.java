package org.pr.dfs.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Set;

@Data
public class SearchRequest {
    private String query;
    private String fileName;
    private String contentType;
    private Long minSize;
    private Long maxSize;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime  fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;

    private Set<String> tags;

    private String sortBy = "uploadTime";
    private String sortDirection = "desc";

    private int page = 0;
    private int size = 20;
}
