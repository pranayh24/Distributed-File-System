package org.prh.DFS.client;

import org.prh.DFS.model.FileChunk;
import org.prh.DFS.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.*;

public class Client {
    public static final int MAX_RETRIES = 3;
    private static final int THREAD_POOL_SIZE = 5;
    private final String serverAddress;
    private final int port;
    private final ExecutorService executorService;

    public Client(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    public void uploadFile(String localPath) throws IOException {
        File file = new File(localPath);
        if(!file.exists()) {
            throw new FileNotFoundException("Source file not found: " + localPath);
        }

        String fileId = UUID.randomUUID().toString();
        String transferId = FileUtils.initializeTransfer(file, FileUtils.DEFAULT_CHUNK_SIZE);

        try (FileInputStream fis = new FileInputStream(file)) {
            long totalChunks = (file.length() + FileUtils.DEFAULT_CHUNK_SIZE - 1) / FileUtils.DEFAULT_CHUNK_SIZE;
            CompletableFuture<?>[] futures = new CompletableFuture[(int) totalChunks];


            for(int i = 0;i < totalChunks; i++) {
                final int chunkNumber = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    try {
                        uploadChunk(file, fileId, chunkNumber, totalChunks);
                        FileUtils.getProgress(transferId).incrementProcessedChunks();

                        double progress = FileUtils.getProgress(transferId).getProgress();
                        System.out.printf("\rUpload progress: %.2f%", progress);
                    } catch(IOException e) {
                        throw new CompletionException(e);
                    }
                }, executorService);
            }

            CompletableFuture.allOf(futures).join();
            System.out.println("\nUpload complete!");
        } finally {
            FileUtils.removeProgress(transferId);
        }
    }

    private void uploadChunk(File file, String fileId, int chunkNumber, long totalChunks) throws IOException {
        try (Socket socket = new Socket(serverAddress, port);
             RandomAccessFile raf = new RandomAccessFile(file, "r");
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Position the file pointer
            long position = (long) chunkNumber * FileUtils.DEFAULT_CHUNK_SIZE;
            raf.seek(position);

            // Read the chunk
            int chunkSize = (int) Math.min(FileUtils.DEFAULT_CHUNK_SIZE, file.length() - position);
            byte[] data = new byte[chunkSize];
            raf.read(data);

            // Calculate checksum
            String checksum = FileUtils.calculateCheckSum(data);

            // Create and send chunk
            FileChunk chunk = new FileChunk(fileId, file.getName(), chunkNumber,
                    data, checksum, totalChunks);
            oos.writeObject(chunk);
            oos.flush();

            // Wait for server confirmation
            String response = (String) ois.readObject();
            if (!response.equals("CHUNK_RECEIVED")) {
                throw new IOException("Failed to upload chunk " + chunkNumber + ": " + response);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public void close() {
        executorService.shutdown();
        try {
            if(!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch(InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 8888);
        try {
            client.uploadFile("D:\\seriess\\cricket_prediction_system.py");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.close();
        }
    }
}
