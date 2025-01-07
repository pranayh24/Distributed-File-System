// Update src/main/java/org/prh/dfs/model/Command.java
package org.pr.dfs.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class Command implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        // Existing commands
        LIST_DIR,
        CREATE_DIR,
        DELETE_DIR,
        UPLOAD_FILE,
        DOWNLOAD_FILE,
        DELETE_FILE,
        MOVE,
        RENAME,

        // New version control commands
        CREATE_VERSION,
        LIST_VERSIONS,
        RESTORE_VERSION,
        COMPARE_VERSIONS,
        DELETE_VERSION,

        SHOW_REPLICATION_STATUS, // Replication command
        FORCE_REPLICATION,
        SHOW_NODE_HEALTH,
        HEARTBEAT
    }

    private final Type type;
    private final String path;
    private String newPath;           // Used for MOVE and RENAME operations

    // New fields for version control
    private String versionId;         // Version identifier
    private String creator;           // User who created the version
    private String comment;           // Version comment/description
    private String compareVersionId;  // Used for version comparison
    private boolean includeMetadata;  // Whether to include version metadata
    private int maxVersions;          // Maximum versions to retrieve/keep


    // Basic constructor for simple commands
    public Command(Type type, String path) {
        this(type, path, null);
    }

    public Command(Type type) {
        this(type, null);
    }

    // Constructor for operations requiring two paths
    public Command(Type type, String path, String newPath) {
        this.type = type;
        this.path = normalizePath(path);
        this.newPath = newPath != null ? normalizePath(newPath) : null;
    }

    // New constructor for version control operations
    public Command(Type type, String path, String versionId, String creator, String comment) {
        this(type, path);
        this.versionId = versionId;
        this.creator = creator;
        this.comment = comment;
    }

    // Builder method for version comparison
    public Command withVersionComparison(String compareVersionId) {
        this.compareVersionId = compareVersionId;
        return this;
    }

    // Builder method for metadata inclusion
    public Command withMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
        return this;
    }

    // Builder method for max versions limit
    public Command withMaxVersions(int maxVersions) {
        this.maxVersions = maxVersions;
        return this;
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

    // Helper method to validate version-related commands
    public boolean isVersioningCommand() {
        return type == Type.CREATE_VERSION ||
                type == Type.LIST_VERSIONS ||
                type == Type.RESTORE_VERSION ||
                type == Type.COMPARE_VERSIONS ||
                type == Type.DELETE_VERSION;
    }

    // Helper method to check if command requires a version ID
    public boolean requiresVersionId() {
        return type == Type.RESTORE_VERSION ||
                type == Type.DELETE_VERSION ||
                type == Type.COMPARE_VERSIONS;
    }

    // Validate command based on its type
    public void validate() throws IllegalArgumentException {
        if (path == null) {
            throw new IllegalArgumentException("Path is required");
        }

        if (requiresVersionId() && versionId == null) {
            throw new IllegalArgumentException("Version ID is required for " + type);
        }

        if (type == Type.MOVE || type == Type.RENAME) {
            if (newPath == null) {
                throw new IllegalArgumentException("New path is required for " + type);
            }
        }

        if (type == Type.CREATE_VERSION && creator == null) {
            throw new IllegalArgumentException("Creator is required for creating version");
        }

        if (type == Type.COMPARE_VERSIONS && compareVersionId == null) {
            throw new IllegalArgumentException("Compare version ID is required for version comparison");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder()
                .append("Command{type=").append(type)
                .append(", path='").append(path).append('\'');

        if (newPath != null) sb.append(", newPath='").append(newPath).append('\'');
        if (versionId != null) sb.append(", versionId='").append(versionId).append('\'');
        if (creator != null) sb.append(", creator='").append(creator).append('\'');
        if (comment != null) sb.append(", comment='").append(comment).append('\'');
        if (compareVersionId != null) sb.append(", compareVersionId='").append(compareVersionId).append('\'');

        sb.append(", includeMetadata=").append(includeMetadata);
        if (maxVersions > 0) sb.append(", maxVersions=").append(maxVersions);

        return sb.append('}').toString();
    }
}