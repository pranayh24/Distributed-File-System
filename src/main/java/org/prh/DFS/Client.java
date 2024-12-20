package org.prh.DFS;

import java.io.*;
import java.net.*;

public class Client {
    private static final int BUFFER_SIZE = 4096;
    private static final int SOCKET_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_RETRIES = 3;

    private final String serverAddress;
    private final int port;
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    public Client(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket();
        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.setKeepAlive(true);
        socket.connect(new InetSocketAddress(serverAddress, port), SOCKET_TIMEOUT);
        dis = new DataInputStream(socket.getInputStream());
        dos = new DataOutputStream(socket.getOutputStream());
    }

    public void close() {
        try {
            if (dos != null) {
                dos.writeUTF("EXIT");
                dos.flush();
            }
        } catch (IOException e) {
            // Ignore error on exit
        }

        try {
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    public void uploadFile(String localPath, String remoteName) throws IOException {
        File file = new File(localPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Source file not found: " + localPath);
        }

        dos.writeUTF("UPLOAD");
        dos.writeUTF(remoteName);
        dos.writeLong(file.length());
        dos.flush();

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush();
        }

        String response = dis.readUTF();
        if (!response.equals("UPLOAD SUCCESSFUL")) {
            throw new IOException("Upload failed: " + response);
        }
        System.out.println("Upload successful: " + remoteName);
    }

    public void downloadFile(String remoteName, String localPath) throws IOException {
        dos.writeUTF("DOWNLOAD");
        dos.writeUTF(remoteName);
        dos.flush();

        String response = dis.readUTF();
        if (!response.equals("DOWNLOAD READY")) {
            throw new IOException("Download failed: " + response);
        }

        long fileSize = dis.readLong();
        try (FileOutputStream fos = new FileOutputStream(localPath)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long remaining = fileSize;
            while (remaining > 0) {
                int bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                if (bytesRead < 0) {
                    throw new IOException("Unexpected end of stream");
                }
                fos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }
        }
        System.out.println("Download successful: " + remoteName);
    }

    public void deleteFile(String remoteName) throws IOException {
        dos.writeUTF("DELETE");
        dos.writeUTF(remoteName);
        dos.flush();

        String response = dis.readUTF();
        if (!response.equals("DELETE SUCCESSFUL")) {
            throw new IOException("Delete failed: " + response);
        }
        System.out.println("Delete successful: " + remoteName);
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 8888);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                client.connect();

                // Perform operations
                client.uploadFile("D:\\seriess\\file.txt", "test.txt");
                client.downloadFile("test.txt", "D:\\seriess\\downloaded_test.txt");
                client.deleteFile("test.txt");

                break; // Success - exit the retry loop
            } catch (Exception e) {
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                e.printStackTrace();

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(2000 * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } finally {
                client.close();
            }
        }
    }
}