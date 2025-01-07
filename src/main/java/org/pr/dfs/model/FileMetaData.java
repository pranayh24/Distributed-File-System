package org.pr.dfs.model;

import lombok.Getter;

import java.io.Serializable;
import java.util.Date;

@Getter
public class FileMetaData implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String path;
    private final boolean isDirectory;
    private final long size;
    private final Date lastModified;

    public FileMetaData(String name, String path, boolean isDirectory, long size, Date lastModified) {
        this.name = name;
        this.path = path;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastModified = lastModified;
    }
}
