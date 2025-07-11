package org.pr.dfs.service;

import org.pr.dfs.dto.FileMetadata;
import org.pr.dfs.dto.SearchRequest;
import org.pr.dfs.dto.SearchResult;

import java.util.List;

public interface SearchService {
    SearchResult searchFiles(SearchRequest request) throws Exception;
    SearchResult searchByTag(String userId, String tag, int page, int size)  throws Exception;
    SearchResult getRecentFiles(String userId, int page, int size) throws Exception;
    SearchResult getPopularFiles(String userId, int page, int size) throws Exception;
    List<String> getFileNameSuggestions(String userId, String query) throws Exception;
    void saveFileMetadata(FileMetadata fileMetadata) throws Exception;
    void updateFileAccess(String filePath) throws Exception;
    void deleteFileMetadata(String filePath) throws Exception;
    FileMetadata getFileMetadataByPath(String filePath) throws Exception;
}
