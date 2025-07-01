package org.pr.dfs.service;

import org.pr.dfs.dto.FileMetaDataDto;
import org.pr.dfs.dto.FileUploadRequest;
import org.springframework.core.io.Resource;

public interface FileService {
    FileMetaDataDto uploadFile(FileUploadRequest request) throws Exception;
    Resource downloadFile(String filePath) throws Exception;
    FileMetaDataDto getFileMetaData(String fileName) throws Exception;
    boolean deleteFile(String filePath) throws Exception;
}
