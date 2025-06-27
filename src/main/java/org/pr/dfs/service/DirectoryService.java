package org.pr.dfs.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.pr.dfs.dto.FileMetaDataDto;

import java.util.List;

public interface DirectoryService {

    List<FileMetaDataDto> listDirectory(String path) throws Exception;
    void createDirectory(String path) throws Exception;
    void deleteDirectory(String path) throws Exception;
    void moveOrRename(String sourcePath, String destinationPath) throws Exception;
}
