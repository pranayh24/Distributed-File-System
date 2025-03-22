package org.pr.dfs.client;

import org.pr.dfs.model.Command;
import org.pr.dfs.model.FileOperationResult;
import org.pr.dfs.model.Version;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Logger;

public class VersionOperations {
    private static final Logger LOGGER = Logger.getLogger(VersionOperations.class.getName());

    private String serverAddress;
    private int port;

    public VersionOperations(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public VersionOperations(DFSClientCommunicator communicator) {
    }

    public FileOperationResult createVersion(String filePath, String comment) {
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Remove any quotes from the comment
            comment = comment.replaceAll("\"","");

            Command command = new Command(Command.Type.CREATE_VERSION, filePath);
            command.setComment(comment);
            command.setCreator(System.getProperty("user.name"));

            //Send the command
            oos.writeObject(command);
            oos.flush();

            // Read and return the response
            Object response = ois.readObject();
            if(response instanceof FileOperationResult) {
                return (FileOperationResult) response;
            } else {
                LOGGER.warning("Unexpected response type: " + response.getClass().getName());
                return FileOperationResult.error("Unexpected response from server");
            }
        } catch(Exception e) {
            LOGGER.severe("Error creating version: " + e.getMessage());
            return FileOperationResult.error("Failed to create version: " + e.getMessage());
        }
    }

    public List<Version> listVersions(String filePath) throws IOException{
        try(Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.LIST_VERSIONS, filePath);
            oos.writeObject(command);

            Object  response = ois.readObject();
            if(response instanceof FileOperationResult) {
                FileOperationResult result = (FileOperationResult) response;
                if(result.isSuccess() && result.getData() instanceof List) {
                    return (List<Version>) result.getData();
                }
                throw new IOException(result.getMessage());
            }
            throw new IOException(("Unexpected response type"));
        } catch(ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public FileOperationResult restoreVersion(String filePath, String versionId) {
        try (Socket socket = new Socket(serverAddress, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.RESTORE_VERSION, filePath,
                    versionId, System.getProperty("user.name"), null);
            oos.writeObject(command);

            Object response = ois.readObject();
            if(response instanceof  FileOperationResult) {
                return (FileOperationResult) response;
            }
            return FileOperationResult.error("Unexpected response type");
        } catch(Exception e) {
            LOGGER.severe("Error restoring version: " + e.getMessage());
            return FileOperationResult.error(e);
        }
    }
}
