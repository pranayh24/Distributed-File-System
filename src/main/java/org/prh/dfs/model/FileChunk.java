package org.prh.dfs.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class FileChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String fileId;
    private final int chunkNumber;
    private final byte[] data;
    private final String checksum;
    private final long totalChunks;
    private final String fileName;

    public FileChunk(String fileId, String fileName, int chunkNumber, byte[] data, String checksum, long totalChunks) {
        this.fileId = fileId;
        this.chunkNumber = chunkNumber;
        this.data = data;
        this.checksum = checksum;
        this.totalChunks = totalChunks;
        this.fileName = fileName;
    }


}
