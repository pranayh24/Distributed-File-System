package org.prh.DFS.server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private final String storagePath;
    private static final int BUFFER_SIZE = 4096;

    public ClientHandler(Socket clientSocket, String storagePath) {
        this.clientSocket = clientSocket;
        this.storagePath = storagePath.endsWith(File.separator) ? storagePath : storagePath + File.separator;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            while (!clientSocket.isClosed()) {
                try {
                    String command = dis.readUTF();
                    System.out.println("Command received: " + command);

                    switch (command.toUpperCase()) {
                        case "UPLOAD":
                            handleUpload(dis, dos);
                            break;
                        case "DOWNLOAD":
                            handleDownload(dis, dos);
                            break;
                        case "DELETE":
                            handleDelete(dis, dos);
                            break;
                        case "EXIT":
                            return;
                        default:
                            dos.writeUTF("ERROR: Invalid command");
                    }
                    dos.flush();
                } catch (EOFException e) {
                    // Client disconnected
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();

        File file = new File(storagePath + fileName);
        try (FileOutputStream fos = new FileOutputStream(file)) {
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

            dos.writeUTF("UPLOAD SUCCESSFUL");
            dos.flush();
            System.out.println("File uploaded successfully: " + fileName);
        } catch (IOException e) {
            dos.writeUTF("ERROR: Upload failed - " + e.getMessage());
            dos.flush();
            throw e;
        }
    }

    private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();
        File file = new File(storagePath + fileName);

        if (!file.exists()) {
            dos.writeUTF("ERROR: File not found");
            dos.flush();
            return;
        }

        try {
            dos.writeUTF("DOWNLOAD READY");
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
            System.out.println("File sent successfully: " + fileName);
        } catch (IOException e) {
            System.err.println("Error sending file: " + e.getMessage());
            throw e;
        }
    }

    private void handleDelete(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF();
        File file = new File(storagePath + fileName);

        if (file.exists() && file.delete()) {
            dos.writeUTF("DELETE SUCCESSFUL");
        } else {
            dos.writeUTF("ERROR: File not found or could not be deleted");
        }
        dos.flush();
    }
}