package org.prh.dfs.fault;

import org.prh.dfs.model.Command;
import org.springframework.boot.logging.LoggerGroup;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.logging.Logger;

public class HeartbeatSender {
    private static final Logger LOGGER = Logger.getLogger(HeartbeatSender.class.getName());
    private final String host;
    private final int port;

    public HeartbeatSender(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendHeartbeat() throws IOException{
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            Command heartbeatCommand = new Command(Command.Type.HEARTBEAT);
            oos.writeObject(heartbeatCommand);
            oos.flush();

        } catch (IOException e) {
            LOGGER.warning("Failed to send heartbeat: " + e.getMessage());
            throw e;
        }
    }
}
