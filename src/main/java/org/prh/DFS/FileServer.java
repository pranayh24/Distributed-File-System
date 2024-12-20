package org.prh.DFS;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final int port;
    private final String storagePath;
    private ServerSocket serverSocket;
    private boolean running = true;

    public FileServer(int port, String storagePath) {
        this.port = port;
        this.storagePath = storagePath;
        File storageDir = new File(storagePath);

        if(!storageDir.exists()) {
            storageDir.mkdirs();
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("File server started on port: " + port);

            while(running) {
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Handle requests in a separate thread
                    new Thread(new ClientHandler(clientSocket, storagePath)).start();
                } catch (Exception e) {
                    if(running) {
                        System.err.println("Error handling client request: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        if(serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("Server stopped");
            } catch (Exception e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int port = 8888;
        String storagePath = "D:\\Server Path";

        FileServer fileServer = new FileServer(port, storagePath);
        fileServer.start();
    }
}
