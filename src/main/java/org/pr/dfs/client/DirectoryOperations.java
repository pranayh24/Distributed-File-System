package org.pr.dfs.client;

import org.pr.dfs.model.Command;
import org.pr.dfs.model.FileMetaData;
import org.pr.dfs.model.FileOperationResult;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class DirectoryOperations {
    private static final Logger LOGGER = Logger.getLogger(DirectoryOperations.class.getName());

    private final DFSClientCommunicator  communicator;


    public DirectoryOperations(DFSClientCommunicator communicator) {
        this.communicator = communicator;
    }

    @SuppressWarnings("unchecked")
    public List<FileMetaData> listDirectory(String path) throws IOException, ClassNotFoundException {
        path = path.replaceAll("\"", "");
        Command command = new Command(Command.Type.LIST_DIRECTORY, path);

        FileOperationResult result = communicator.sendCommand(command);
        if (result.getData() instanceof List) {
            return (List<FileMetaData>) result;
        } else if (result.getData() instanceof String) {
            throw new IOException("Server error: " + result);
        } else {
            throw new IOException("Unexpected response type from server");
        }
    }

    public boolean createDirectory(String path) throws IOException, ClassNotFoundException {
        path = path.replaceAll("\"", "");
        Command command = new Command(Command.Type.CREATE_DIRECTORY, path);

        FileOperationResult result = communicator.sendCommand(command);
        if (result.getData() instanceof Boolean) {
            return (boolean) result.getData();
        } else if (result.getData() instanceof String) {
            throw new IOException("Server error: " + result);
        } else {
            throw new IOException("Unexpected response type from server");
        }
    }

    public boolean deleteDirectory(String path) throws IOException, ClassNotFoundException {
        path = path.replaceAll("\"", "");
        Command command = new Command(Command.Type.DELETE_DIRECTORY, path);

        FileOperationResult result = communicator.sendCommand(command);
        if (result.getData() instanceof Boolean) {
            return (boolean) result.getData();
        } else if (result.getData() instanceof String) {
            throw new IOException("Server error: " + result);
        } else {
            throw new IOException("Unexpected response type from server");
        }
    }

    public boolean moveOrRename(String sourcePath, String destinationPath) throws IOException, ClassNotFoundException {
        sourcePath = sourcePath.replaceAll("\"", "");
        destinationPath = destinationPath.replaceAll("\"", "");
        Command command = new Command(Command.Type.MOVE_RENAME, sourcePath, destinationPath);

        FileOperationResult result = communicator.sendCommand(command);
        if (result.getData() instanceof Boolean) {
            return (boolean) result.getData();
        } else if (result.getData() instanceof String) {
            throw new IOException("Server error: " + result);
        } else {
            throw new IOException("Unexpected response type from server");
        }
    }
}