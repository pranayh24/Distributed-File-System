package org.prh.dfs.client;

import org.prh.dfs.model.Command;
import org.prh.dfs.model.FileMetaData;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class DirectoryOperations {
    private final String serverAddress;
    private final int port;

    public DirectoryOperations(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public List<FileMetaData> listDirectory(String path) throws IOException{
        try(Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.DELETE_DIR, path);
            oos.writeObject(command);

            @SuppressWarnings("unchecked")
            List<FileMetaData> files = (List<FileMetaData>) ois.readObject();
            return files;

        } catch(ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }


    public boolean createDirectory(String path) throws IOException {
        try(Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.CREATE_DIR, path);
            oos.writeObject(command);

            return (Boolean) ois.readObject();
        } catch(ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public boolean deleteDirectory(String path) throws IOException {
        try(Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.DELETE_DIR, path);
            oos.writeObject(command);

            return (Boolean) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw  new IOException("Error reading server response", e);
        }
    }

    public boolean moveOrRename(String sourcePath, String destinationPath) throws IOException {
        try(Socket socket = new Socket(serverAddress, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.MOVE, sourcePath, destinationPath);
            oos.writeObject(command);

            return (Boolean) ois.readObject();
        } catch(ClassNotFoundException e) {
            throw  new IOException("Error reading server response", e);
        }
    }
}
