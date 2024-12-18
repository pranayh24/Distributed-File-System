package org.prh.DFS;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final String storagePath;

    public ClientHandler(Socket clientSocket, String storagePath) {
        this.clientSocket = clientSocket;
        this.storagePath = storagePath;
    }

    @Override
    public void run() {
        try(DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            String command = dis.readUTF(); // Read the command
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
                default:
                    System.out.println("ERROR: Invalid command");
            }

        } catch(Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }

    private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException{
        String fileName = dis.readUTF(); // File name
        long fileSize = dis.readLong(); // File size

        File file = new File(storagePath + fileName);
        try(FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            long remaining = fileSize;

            while(remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                fos.write(buffer, 0, bytesRead);
                remaining -= bytesRead;
            }

            dos.writeUTF("UPLOAD SUCCESSFUL");
        } catch(Exception e) {
            dos.writeUTF("ERROR: Upload failed.");
        }
    }

    private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException{
        String fileName = dis.readUTF(); // File name
        File file = new File(storagePath + fileName);

        if(!file.exists()) {
            dos.writeUTF("ERROR: File not found");
            return;
        }

        dos.writeUTF("DOWNLOAD READY");
        dos.writeLong(file.length()); // Send the file size

        try(FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while((bytesRead = fis.read(buffer))>0) {
                dos.write(buffer, 0, bytesRead);
            }
        }
    }

    private void handleDelete(DataInputStream dis, DataOutputStream dos) throws IOException {
        String fileName = dis.readUTF(); //File name
        File file = new File(storagePath + fileName);

        if(file.exists() && file.delete()) {
            dos.writeUTF("DELETE SUCCESSFUL");
        } else {
            dos.writeUTF("ERROR: File not found or could not be deleted");
        }

    }


}
