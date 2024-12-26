package org.prh.dfs.client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.prh.dfs.model.Command;
import org.prh.dfs.model.FileChunk;
import org.prh.dfs.model.FileMetaData;
import org.prh.dfs.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.logging.Logger;

public class DFSClient {
    private static final Logger LOGGER = Logger.getLogger(DFSClient.class.getName());
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks

    private static final String ANSI_RESET  = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private final String serverAddress;
    private final int serverPort;
    private final DirectoryOperations dirOps;
    private final SimpleDateFormat dateFormat;
    private final LineReader lineReader;

    public DFSClient(String serverAddress, int serverPort) throws IOException{
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.dirOps = new DirectoryOperations(serverAddress, serverPort);
        this.dateFormat = new SimpleDateFormat("dd MMM HH:mm");

        Terminal terminal = TerminalBuilder.builder().system(true).build();
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

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

    private void showPrompt() {
        System.out.println("\n" + ANSI_GREEN + "╔════════════════════════════════╗");
        System.out.println("║     Distributed File System     ║");
        System.out.println("╠════════════════════════════════╣");
        System.out.println("║ 1. List Directory              ║");
        System.out.println("║ 2. Create Directory            ║");
        System.out.println("║ 3. Delete Directory            ║");
        System.out.println("║ 4. Upload File                 ║");
        System.out.println("║ 5. Download File               ║");
        System.out.println("║ 6. Move/Rename File/Directory  ║");
        System.out.println("║ 7. Exit                        ║");
        System.out.println("╚════════════════════════════════╝" + ANSI_RESET);
    }

    private void listDirectory() throws IOException{
        String path = lineReader.readLine("Enter path to list (or press Enter for root): ");
        if(path.isEmpty()) {
            path = "/";
        }

        List<FileMetaData> files = dirOps.listDirectory(path);

        if(files.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Directory is empty" + ANSI_RESET);
            return;
        }

        System.out.println("\n" + ANSI_BLUE + String.format("%-40s %-8s %12s %-20s", "Name", "Type", "Size", "Last Modified") + ANSI_RESET);
        System.out.println("-".repeat(80));

        for(FileMetaData file: files) {
            System.out.printf("%-40s %-8s %-12s\n",
                    file.getName(),
                    file.isDirectory() ? "DIR" : "FILE",
                    formatFileSize(file.getSize()),
                    dateFormat.format(file.getLastModified()));
        }
    }

    private String formatFileSize(long size) {
        if(size < 1024) return size + "B";
        if(size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if(size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private void showProgressBar(long current, long total, String operation) {
        int width = 50;
        int progress = (int) ((current * width) / total);
        System.out.print("\r" + operation + " [");
        for(int i = 0;i < width; i++) {
            System.out.print(i < progress ? "=": " ");
        }
        System.out.printf("] %.1f%%", (current * 100.0) / total);
    }

    public void start() {
        while(true) {
            try {
                showPrompt();
                String choice = lineReader.readLine("Choose an option: ");

                switch(choice) {
                    case "1" -> listDirectory();
                    case "2" -> {
                        String path  = lineReader.readLine("Enter directory path to create: ");
                        boolean created = dirOps.createDirectory(path);
                        System.out.println(created ?
                                ANSI_GREEN + "Directory created successfully" + ANSI_RESET :
                                ANSI_YELLOW + "Failed to create directory" + ANSI_RESET);
                    }
                    case "3" -> {
                        String path = lineReader.readLine("Enter directory path to delete: ");
                        boolean deleted = dirOps.deleteDirectory(path);
                        System.out.println(deleted ?
                                ANSI_GREEN + "Directory deleted successfully" + ANSI_RESET :
                                ANSI_YELLOW + "Failed to delete directory" + ANSI_RESET);
                    }
                    case "4" -> {
                        String localPath = lineReader.readLine("Enter local file Path");
                        String remotePath = lineReader.readLine("Enter remote  directory path (or press Enter for root): ");
                        if (remotePath.isEmpty()) remotePath = "/";
                        uploadFile(localPath, remotePath);
                    }
                    case "5" -> {
                        String remotePath = lineReader.readLine("Enter remote file path: ");
                        String localPath = lineReader.readLine("Enter local file path: ");
                        downloadFile(localPath, remotePath);
                    }
                    case "6" -> {
                        String sourcePath = lineReader.readLine("Enter remote file Path: ");
                        String destPath = lineReader.readLine("Enter destination path: ");
                        boolean moved  = dirOps.moveOrRename(sourcePath, destPath);
                        System.out.println(moved ?
                                ANSI_GREEN + "Move/rename successful" + ANSI_RESET :
                                ANSI_YELLOW + "Failed to move/rename" + ANSI_RESET);
                    }
                    case "7" -> {
                        System.out.println(ANSI_GREEN+ "Thank you for using DFS. Goodbye!" + ANSI_RESET);
                        return;
                    }
                    default -> System.out.println(ANSI_YELLOW + "Invalid option, please try again" + ANSI_RESET);

                }
            } catch(IOException e) {
                System.out.println(ANSI_YELLOW + "Error: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    public static void main(String[] args) {
        try {
            DFSClient client  = new DFSClient("localhost", 8888);
            client.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }
}