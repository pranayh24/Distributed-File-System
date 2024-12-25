package org.prh.dfs.client;

import org.prh.dfs.model.Command;
import org.prh.dfs.model.FileChunk;
import org.prh.dfs.model.FileMetaData;
import org.prh.dfs.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DFSClient {
    private static final Logger LOGGER = Logger.getLogger(DFSClient.class.getName());
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    private final String serverAddress;
    private final int serverPort;
    private final DirectoryOperations dirOps;

    public DFSClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.dirOps = new DirectoryOperations(serverAddress, serverPort);
    }

    public void uploadFile(String localPath, String remotePath) throws IOException {
        // Remove quotes if present
        localPath = localPath.replaceAll("\"", "");
        remotePath = remotePath.replaceAll("\"", "");

        File file = new File(localPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Local file not found: " + localPath);
        }

        // Get just the filename for the remote path
        String fileName = file.getName();
        String fullRemotePath = remotePath.endsWith("/") ? remotePath + fileName : remotePath + "/" + fileName;

        long fileSize = file.length();
        int totalChunks = (int) Math.ceil(fileSize / (double) CHUNK_SIZE);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int chunkNumber = 0;

            while (true) {
                int bytesRead = fis.read(buffer);
                if (bytesRead == -1) break;

                byte[] chunkData;
                if (bytesRead < CHUNK_SIZE) {
                    chunkData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                } else {
                    chunkData = buffer.clone();
                }

                FileChunk chunk = new FileChunk(
                        fileName,
                        fullRemotePath,
                        chunkNumber,
                        chunkData,
                        FileUtils.calculateCheckSum(chunkData),
                        totalChunks
                );

                sendChunk(chunk);
                System.out.printf("Sent chunk %d of %d for file %s%n",
                        chunkNumber + 1, totalChunks, fileName);

                chunkNumber++;
            }
            System.out.println("File upload completed successfully!");
        }
    }

    private void sendChunk(FileChunk chunk) throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            LOGGER.info("Sending chunk: " + chunk.getChunkNumber());
            oos.writeObject(chunk);
            oos.flush(); // Ensure all data is sent

            String response = (String) ois.readObject();
            LOGGER.info("Received response: " + response);

            if (!"CHUNK_RECEIVED".equals(response)) {
                throw new IOException("Failed to send chunk: " + response);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    public void downloadFile(String remotePath, String localPath) throws IOException {
        remotePath = remotePath.replaceAll("\"", "");
        localPath = localPath.replaceAll("\"", "");

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.DOWNLOAD_FILE, remotePath);
            oos.writeObject(command);

            try (FileOutputStream fos = new FileOutputStream(localPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = ois.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        DFSClient client = new DFSClient("localhost", 8888);

        while (true) {
            try {
                System.out.println("\nDFS Client Menu:");
                System.out.println("1. List Directory");
                System.out.println("2. Create Directory");
                System.out.println("3. Delete Directory");
                System.out.println("4. Upload File");
                System.out.println("5. Download File");
                System.out.println("6. Move/Rename File/Directory");
                System.out.println("7. Exit");
                System.out.print("Choose an option: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        System.out.print("Enter path to list (or press Enter for root): ");
                        String listPath = scanner.nextLine().trim();
                        if (listPath.isEmpty()) {
                            listPath = "/";
                        }
                        List<FileMetaData> files = client.dirOps.listDirectory(listPath);
                        System.out.println("\nDirectory contents:");
                        for (FileMetaData file : files) {
                            System.out.println(String.format("%s %s %d bytes %s\n",
                                    file.isDirectory() ? "DIR" : "FILE",
                                    file.getName(),
                                    file.getSize(),
                                    file.getLastModified()));
                        }
                        break;
                    case "2":
                        System.out.print("Enter directory path to create: ");
                        String createPath = scanner.nextLine().replaceAll("\"", "");
                        boolean created = client.dirOps.createDirectory(createPath);
                        System.out.println(created ? "Directory created successfully" : "Failed to create directory");
                        break;

                    case "3":
                        System.out.print("Enter directory path to delete: ");
                        String deletePath = scanner.nextLine().replaceAll("\"", "");
                        boolean deleted = client.dirOps.deleteDirectory(deletePath);
                        System.out.println(deleted ? "Directory deleted successfully" : "Failed to delete directory");
                        break;

                    case "4":
                        System.out.print("Enter local file path: ");
                        String localPath = scanner.nextLine().trim();
                        System.out.print("Enter remote directory path (or press Enter for root): ");
                        String uploadRemotePath = scanner.nextLine().trim();
                        if (uploadRemotePath.isEmpty()) {
                            uploadRemotePath = "/";
                        }
                        client.uploadFile(localPath, uploadRemotePath);
                        break;

                    case "5":
                        System.out.print("Enter remote file path: ");
                        String downloadRemotePath = scanner.nextLine().replaceAll("\"", "");
                        System.out.print("Enter local path to save: ");
                        String downloadLocalPath = scanner.nextLine().replaceAll("\"", "");
                        client.downloadFile(downloadRemotePath, downloadLocalPath);
                        System.out.println("File downloaded successfully");
                        break;

                    case "6":
                        System.out.print("Enter source path: ");
                        String sourcePath = scanner.nextLine().replaceAll("\"", "");
                        System.out.print("Enter destination path: ");
                        String destPath = scanner.nextLine().replaceAll("\"", "");
                        boolean moved = client.dirOps.moveOrRename(sourcePath, destPath);
                        System.out.println(moved ? "Move/rename successful" : "Failed to move/rename");
                        break;

                    case "7":
                        System.out.println("Exiting...");
                        return;

                    default:
                        System.out.println("Invalid option, please try again");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in client operation", e);
                System.out.println("Error: " + e.getMessage());
            }
        }
    }
}