package org.pr.dfs.client;

import org.pr.dfs.model.Command;
import org.pr.dfs.model.FileMetaData;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class DirectoryOperations {
    private static final Logger LOGGER = Logger.getLogger(DirectoryOperations.class.getName());

    private final String serverAddress;
    private final int port;

    public DirectoryOperations(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    @SuppressWarnings("unchecked")
    public List<FileMetaData> listDirectory(String path) throws IOException {
        path = path.replaceAll("\"", "");
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.LIST_DIR, path);
            oos.writeObject(command);

            Object response = ois.readObject();
            if (response instanceof List) {
                return (List<FileMetaData>) response;
            } else if (response instanceof String) {
                throw new IOException("Server error: " + response);
            } else {
                throw new IOException("Unexpected response type from server");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public boolean createDirectory(String path) throws IOException {
        path = path.replaceAll("\"", "");
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.CREATE_DIR, path);
            oos.writeObject(command);

            Object response = ois.readObject();
            if (response instanceof Boolean) {
                return (Boolean) response;
            } else if (response instanceof String) {
                throw new IOException("Server error: " + response);
            } else {
                throw new IOException("Unexpected response type from server");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public boolean deleteDirectory(String path) throws IOException {
        path = path.replaceAll("\"", "");
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.DELETE_DIR, path);
            oos.writeObject(command);

            Object response = ois.readObject();
            if (response instanceof Boolean) {
                return (Boolean) response;
            } else if (response instanceof String) {
                throw new IOException("Server error: " + response);
            } else {
                throw new IOException("Unexpected response type from server");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public boolean moveOrRename(String sourcePath, String destinationPath) throws IOException {
        sourcePath = sourcePath.replaceAll("\"", "");
        destinationPath = destinationPath.replaceAll("\"", "");
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.MOVE, sourcePath, destinationPath);
            oos.writeObject(command);

            Object response = ois.readObject();
            if (response instanceof Boolean) {
                return (Boolean) response;
            } else if (response instanceof String) {
                throw new IOException("Server error: " + response);
            } else {
                throw new IOException("Unexpected response type from server");
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }
}