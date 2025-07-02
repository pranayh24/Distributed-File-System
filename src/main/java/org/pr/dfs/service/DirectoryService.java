package org.pr.dfs.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.pr.dfs.dto.FileMetaDataDto;

import java.util.List;

public interface DirectoryService {

    List<FileMetaDataDto> listDirectory(String path) throws Exception;
    boolean createDirectory(String path) throws Exception;
    boolean deleteDirectory(String path) throws Exception;
    boolean moveOrRename(String sourcePath, String destinationPath) throws Exception;
}
