package org.prh.DFS.client;

import org.prh.DFS.model.FileChunk;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.*;
import org.prh.DFS.utils.FileUtils;

public class Client {
    private final String serverAddress;
    private final int port;
    private final ExecutorService executorService;

    public Client(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(5);
    }

    public void uploadFile(String localPath) throws IOException {
        File file = new File(localPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Source file not found: " + localPath);
        }

        System.out.println("Starting upload of file: " + file.getName());
        System.out.println("File size: " + file.length() + " bytes");

        try {
            // Calculate number of chunks
            int chunkSize = 1024 * 1024; // 1MB chunks
            long totalChunks = (file.length() + chunkSize - 1) / chunkSize;
            System.out.println("Total chunks to upload: " + totalChunks);

            // Read and upload each chunk
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[chunkSize];
                int chunkNumber = 0;
                int bytesRead;

                while ((bytesRead = fis.read(buffer)) > 0) {
                    final int currentChunk = chunkNumber;
                    final byte[] chunkData = bytesRead < chunkSize ?
                            java.util.Arrays.copyOf(buffer, bytesRead) : buffer.clone();

                    System.out.println("Uploading chunk " + currentChunk + " of " + totalChunks);

                    // Upload chunk
                    uploadChunk(file.getName(), currentChunk, chunkData, totalChunks);
                    chunkNumber++;
                }
            }

            System.out.println("Upload complete!");
        } finally {
            close();
        }
    }

    private void uploadChunk(String fileName, int chunkNumber, byte[] data, long totalChunks)
            throws IOException {
        try (Socket socket = new Socket(serverAddress, port)) {
            System.out.println("Connected to server for chunk " + chunkNumber);

            try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

                // Calculate checksum
                String checksum = FileUtils.calculateCheckSum(data);

                // Create and send chunk
                FileChunk chunk = new FileChunk(
                        UUID.randomUUID().toString(),
                        fileName,
                        chunkNumber,
                        data,
                        checksum,
                        totalChunks
                );

                oos.writeObject(chunk);
                oos.flush();

                // Wait for server confirmation
                String response = (String) ois.readObject();
                System.out.println("Server response for chunk " + chunkNumber + ": " + response);
            } catch (ClassNotFoundException e) {
                throw new IOException("Error reading server response", e);
            }
        }
    }

    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 8888);
        try {
            // Replace with your actual file path
            String filePath = "D:\\seriess\\cricket_prediction_system.py";
            System.out.println("Starting client...");
            System.out.println("Attempting to upload file: " + filePath);
            client.uploadFile(filePath);
        } catch (IOException e) {
            System.err.println("Error during file upload: ");
            e.printStackTrace();
        }
    }
}