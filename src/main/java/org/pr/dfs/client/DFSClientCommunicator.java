package org.pr.dfs.client;

import org.pr.dfs.model.Command;
import org.pr.dfs.model.FileOperationResult;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class DFSClientCommunicator {
    private final String serverAddress;
    private final int serverPort;

    public DFSClientCommunicator(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public FileOperationResult sendCommand(Command command) throws IOException, ClassNotFoundException{
        try(Socket socket = new Socket(serverAddress, serverPort);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            oos.writeObject(command);
            oos.flush();

            return (FileOperationResult) ois.readObject();
        }
    }
}
