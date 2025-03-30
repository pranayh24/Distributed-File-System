package org.pr.dfs.model;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Command implements Serializable {
    private static final long serialVersionUID = 1L;

    // Command types supported by the system
    public enum Type {
        DOWNLOAD_FILE,
        UPLOAD_FILE,
        LIST_DIRECTORY,
        CREATE_DIRECTORY,
        DELETE_DIRECTORY,
        MOVE_RENAME,
        CREATE_VERSION,
        LIST_VERSIONS,
        RESTORE_VERSION,
        SHOW_REPLICATION_STATUS,
        FORCE_REPLICATION,
        SHOW_NODE_HEALTH,
        RECOVER_NODE,
        ADD_NODE,
        HEARTBEAT
    }

    private Type type;
    private String path;
    private String newPath;
    private String nodeId;
    private String clientId;
    private int replicationFactor;
    private Node nodeDetails;
    private Map<String, Object> parameters;
    private String versionId;
    private String creator;
    private String comment;

    public Command(Type type) {
        this.type = type;
        this.parameters = new HashMap<>();
    }

    public Command(Type type, String path) {
        this(type);
        this.path = path;
    }

    public Command(Type type, String path, String versionId, String creator, Object extraData) {
        this(type, path);
        this.versionId = versionId;
        this.creator = creator;
        if (extraData != null) {
            this.addParameter("extraData", extraData);
        }
    }

    public Command(Type type, String path, String newPath) {
        this(type);
        this.path = path;
        this.newPath = newPath;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Node getNodeDetails() {
        return nodeDetails;
    }

    public void setNodeDetails(Node nodeDetails) {
        this.nodeDetails = nodeDetails;
    }

    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
        addParameter("creator", creator);
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        addParameter("comment", comment);
    }

    @Override
    public String toString() {
        return "Command{" +
                "type=" + type +
                ", path='" + path + '\'' +
                ", newPath='" + newPath + '\'' +
                ", nodeId='" + nodeId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", replicationFactor=" + replicationFactor +
                ", versionId='" + versionId + '\'' +
                '}';
    }
}