package org.prh.dfs.model;

import lombok.Getter;

import java.io.Serializable;

@Getter
public class FileOperationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Object data;
    private final String errorDetails;

    public FileOperationResult(boolean success, String message, Object data, String errorDetails) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.errorDetails = errorDetails;
    }

    public static FileOperationResult success(Object data) {
        return new FileOperationResult(true, "Operation completed successfully", data, null);
    }

    public static FileOperationResult success(String message, Object data) {
        return new FileOperationResult(true, message, data, null);
    }

    public static FileOperationResult error(String message) {
        return new FileOperationResult(false, message, null,null);
    }

    public static FileOperationResult error(String message, String errorDetails) {
        return new FileOperationResult(false, message, null, errorDetails);
    }

    public static FileOperationResult error(Exception e) {
        return new FileOperationResult(false, e.getMessage(), null, e.toString());
    }

    public<T> T getData(Class<T> type) {
        return type.cast(data);
    }

    @Override
    public String toString() {
        if(success) {
            return String.format("Success: %s", message);
        } else {
            return String.format("Error: %s%s", message,
                    errorDetails != null ? " (" + errorDetails + ")" : "");
        }
    }
}
