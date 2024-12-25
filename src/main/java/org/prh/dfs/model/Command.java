package org.prh.dfs.model;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class Command implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        LIST_DIR,
        CREATE_DIR,
        DELETE_DIR,
        UPLOAD_FILE,
        DOWNLOAD_FILE,
        DELETE_FILE,
        MOVE,
        RENAME
    }

    private final Type type;
    private final String path;
    private final String newPath; // Used for MOVE and RENAME operations

    public Command(Type type, String path) {
        this(type, path, null);
    }

    public Command(Type type, String path, String newPath) {
        this.type = type;
        this.path = normalizePath(path);
        this.newPath = newPath != null ? normalizePath(newPath) : null;
    }

    private String normalizePath(String path) {
        if (path == null) return null;
        // Remove quotes if present
        path = path.replaceAll("\"", "");
        // Convert Windows backslashes to forward slashes
        path = path.replace("\\", "/");
        // Remove drive letter if present (D:/ etc)
        path = path.replaceAll("^[A-Za-z]:/", "");
        // Remove leading slash
        path = path.replaceAll("^/+", "");
        return path;
    }
}
