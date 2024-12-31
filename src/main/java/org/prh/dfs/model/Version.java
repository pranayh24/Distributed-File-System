package org.prh.dfs.model;

import lombok.Getter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Getter
public class Version implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String versionId;
    private final String fileName;
    private final String filePath;
    private final long size;
    private final String checksum;
    private final Instant createdAt;
    private final String creator;
    private final String comment;

    public Version(String versionId, String fileName, String filePath, long size,
                   String checksum, Instant createdAt, String creator, String comment) {
        this.versionId = versionId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.size = size;
        this.checksum = checksum;
        this.createdAt = createdAt;
        this.creator = creator;
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return Objects.equals(versionId, version.versionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionId);
    }
}
