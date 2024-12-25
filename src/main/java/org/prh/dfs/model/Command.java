package org.prh.dfs.model;

import lombok.Getter;

@Getter
public class Command {
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
        this.path = path;
        this.newPath = newPath;
    }
}
