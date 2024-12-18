package org.prh.DFS;

import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 8888;

        try (Socket socket = new Socket(serverAddress, port);
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // Test: Upload a file
            dos.writeUTF("UPLOAD");
            dos.writeUTF("test.txt");
            File file = new File("D:\\seriess\\file.txt");
            dos.writeLong(file.length());

            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println(dis.readUTF());

            // Test: Download the same file
            dos.writeUTF("DOWNLOAD");
            dos.writeUTF("test.txt");
            if (dis.readUTF().equals("DOWNLOAD READY")) {
                long fileSize = dis.readLong();
                try (FileOutputStream fos = new FileOutputStream("D:\\seriess\\downloaded_test.txt")) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long remaining = fileSize;

                    while (remaining > 0 && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                        fos.write(buffer, 0, bytesRead);
                        remaining -= bytesRead;
                    }
                }
                System.out.println("File downloaded successfully!");
            }

            // Test: Delete the file
            dos.writeUTF("DELETE");
            dos.writeUTF("test.txt");
            System.out.println(dis.readUTF());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
