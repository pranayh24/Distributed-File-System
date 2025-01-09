package org.pr.dfs.client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.pr.dfs.fault.HeartbeatSender;
import org.pr.dfs.model.*;
import org.pr.dfs.model.*;
import org.pr.dfs.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * DFSClient provides a command-line interface for interacting with a Distributed File System.
 * This client supports file operations, directory management, and version control functionality.
 * It handles file transfers in chunks for better memory management and provides progress feedback
 * for long-running operations.
 */
public class DFSClient {
    private static final Logger LOGGER = Logger.getLogger(DFSClient.class.getName());

    // Configuration constants for file operations
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks for file transfer
    private static final String PATH_SEPARATOR = "/";   // Standard Unix-style separator for remote paths
    private static final int BUFFER_SIZE = 8192;       // Buffer size for file downloads

    // ANSI color codes for terminal output formatting
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    // Client configuration and utilities
    private final String serverAddress;
    private final int serverPort;
    private final DirectoryOperations dirOps;
    private final VersionOperations versionOps;
    private final SimpleDateFormat dateFormat;
    private final LineReader lineReader;
    private final HeartbeatSender heartbeatSender;
    private final ScheduledExecutorService scheduler;

    /**
     * Constructs a new DFS client with specified server connection details.
     * Initializes the terminal interface and required operation handlers.
     *
     * @param serverAddress The address of the DFS server
     * @param serverPort The port number of the DFS server
     * @throws IOException If terminal initialization fails
     */
    public DFSClient(String serverAddress, int serverPort) throws IOException {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.dirOps = new DirectoryOperations(serverAddress, serverPort);
        this.versionOps = new VersionOperations(serverAddress, serverPort);
        this.dateFormat = new SimpleDateFormat("dd MMM HH:mm");

        // Initialize terminal for interactive input
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        this.scheduler = Executors.newScheduledThreadPool(1);
        this.heartbeatSender = new HeartbeatSender(serverAddress, serverPort);

        startHeartBeat();
    }

    private void startHeartBeat() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                heartbeatSender.sendHeartbeat();
            } catch (Exception e) {
                LOGGER.warning("Failed to send heartbeat: " + e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Properly normalizes file paths across different operating systems.
     * Handles both Windows and Unix-style paths correctly.
     *
     * @param path The path to normalize
     * @return Normalized path string
     */
    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }

        // Remove any surrounding quotes
        String cleanPath = path.replaceAll("^\"|\"$", "");

        // For local paths, use system-specific handling
        if (path.contains(":")) {  // Windows absolute path detected
            return Paths.get(cleanPath).toString();
        }

        // For remote paths, convert to forward slashes
        return cleanPath.replace("\\", "/")
                .replaceAll("/+", "/")
                .replaceAll("/$", "");
    }


    /**
     * Uploads a file to the DFS server in chunks.
     * Provides progress feedback during upload.
     *
     * @param localPath Path to the local file
     * @param remotePath Destination path on the DFS
     * @throws IOException If file operations fail
     */
    public void uploadFile(String localPath, String remotePath) throws IOException {
        // Normalize paths for consistent handling
        String normalizedLocalPath = Paths.get(normalizePath(localPath)).toString();
        String normalizedRemotePath = normalizePath(remotePath);

        File file = new File(normalizedLocalPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Local file not found: " + normalizedLocalPath);
        }

        // Construct the full remote path
        String fileName = file.getName();
        String fullRemotePath = normalizedRemotePath.endsWith(PATH_SEPARATOR) ?
                normalizedRemotePath + fileName :
                normalizedRemotePath + PATH_SEPARATOR + fileName;

        long fileSize = file.length();
        int totalChunks = (int) Math.ceil(fileSize / (double) CHUNK_SIZE);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int chunkNumber = 0;

            while (true) {
                int bytesRead = fis.read(buffer);
                if (bytesRead == -1) break;

                // Prepare chunk data
                byte[] chunkData = (bytesRead < CHUNK_SIZE) ?
                        new byte[bytesRead] : buffer.clone();

                if (bytesRead < CHUNK_SIZE) {
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
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
                showProgressBar(chunkNumber + 1, totalChunks, "Uploading");
                chunkNumber++;
            }
            System.out.println("\n" + ANSI_GREEN + "File upload completed successfully!" + ANSI_RESET);
        }
    }

    /**
     * Sends a single chunk of file data to the server.
     *
     * @param chunk The FileChunk to send
     * @throws IOException If network operations fail
     */
    private void sendChunk(FileChunk chunk) throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            LOGGER.info("Sending chunk: " + chunk.getChunkNumber());
            oos.writeObject(chunk);
            oos.flush();

            String response = (String) ois.readObject();
            LOGGER.info("Received response: " + response);

            if (!"CHUNK_RECEIVED".equals(response)) {
                throw new IOException("Failed to send chunk: " + response);
            }
        } catch (ClassNotFoundException e) {
            throw new IOException("Error reading server response", e);
        }
    }

    /**
     * Downloads a file from the DFS server.
     *
     * @param remotePath Path to the file on the server
     * @param localPath Destination path on the local system
     * @throws IOException If file operations fail
     */
    public void downloadFile(String remotePath, String localPath) throws IOException {
        String normalizedRemotePath = normalizePath(remotePath);
        String normalizedLocalPath = Paths.get(normalizePath(localPath)).toString();

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.DOWNLOAD_FILE, normalizedRemotePath);
            oos.writeObject(command);

            try (FileOutputStream fos = new FileOutputStream(normalizedLocalPath)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;
                long totalBytes = 0;
                long fileSize = ois.readLong(); // Read file size from server

                while ((bytesRead = ois.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                    showProgressBar(totalBytes, fileSize, "Downloading");
                }
                System.out.println("\n" + ANSI_GREEN + "File download completed successfully!" + ANSI_RESET);
            }
        }
    }

    /**
     * Manages the creation of file versions with metadata.
     */
    private void createVersion() {
        try {
            String filePath = lineReader.readLine("Enter file path: ");
            String comment = lineReader.readLine("Enter version comment: ");

            // Remove any quotes if present
            filePath = filePath.replaceAll("\"","");
            comment = comment.replaceAll("\"","");

            FileOperationResult result = versionOps.createVersion(normalizePath(filePath), comment);
            if (result.isSuccess()) {
                System.out.println(ANSI_GREEN + "Version created successfully" + ANSI_RESET);
                Version version = (Version) result.getData();
                System.out.println("Version ID: " + version.getVersionId());
            } else {
                System.out.println(ANSI_YELLOW + "Error: " + result.getMessage() + ANSI_RESET);
                if(result.getErrorDetails() != null) {
                    System.out.println("Details: " + result.getErrorDetails());
                }
            }
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error creating version: " + e.getMessage() + ANSI_RESET);
            LOGGER.severe("Error in createVersion: " + e.getMessage());
        }
    }

    /**
     * Lists all versions of a specified file.
     */
    private void listVersions() {
        try {
            String filePath = lineReader.readLine("Enter file path: ");
            List<Version> versions = versionOps.listVersions(normalizePath(filePath));

            if (versions.isEmpty()) {
                System.out.println(ANSI_YELLOW + "No versions found for this file" + ANSI_RESET);
                return;
            }

            System.out.println("\n" + ANSI_BLUE + "File Versions:" + ANSI_RESET);
            System.out.printf("%-36s %-20s %-20s %s%n", "Version ID", "Created At", "Creator", "Comment");
            System.out.println("-".repeat(100));

            for (Version version : versions) {
                System.out.printf("%-36s %-20s %-20s %s%n",
                        version.getVersionId(),
                        version.getCreatedAt().toString(),
                        version.getCreator(),
                        version.getComment());
            }
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error listing versions: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Restores a file to a specific version.
     */
    private void restoreVersion() {
        try {
            String filePath = lineReader.readLine("Enter file path: ");
            String versionId = lineReader.readLine("Enter version ID to restore: ");

            FileOperationResult result = versionOps.restoreVersion(
                    normalizePath(filePath),
                    versionId
            );
            if (result.isSuccess()) {
                System.out.println(ANSI_GREEN + "Version restored successfully" + ANSI_RESET);
            } else {
                System.out.println(ANSI_YELLOW + "Error: " + result.getMessage() + ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error restoring version: " + e.getMessage() + ANSI_RESET);
        }
    }

    public void updateFile() {
        try {
           String localPath = lineReader.readLine("Enter local file path to update: ");
           String remotePath = lineReader.readLine("Enter remote file path: ");
           String comment = lineReader.readLine("Enter version comment");

           localPath = localPath.replaceAll("^/+","");
           remotePath = remotePath.replaceAll("^/+","");

           // Upload the updated file
           uploadFile(localPath, remotePath);

           // Create a version of the updated file
            FileOperationResult result = versionOps.createVersion(remotePath, comment);

            if(result.isSuccess()) {
                System.out.println(ANSI_GREEN + "File updated and version created successfully!" + ANSI_RESET);
                Version version = (Version) result.getData();
                System.out.println("Version ID: " + version.getVersionId());
            } else {
            System.out.println(ANSI_YELLOW + "Error: " + result.getMessage() + ANSI_RESET);
            }
        } catch(IOException e) {
            System.out.println(ANSI_YELLOW + "Error updating and versioning file: " + e.getMessage() + ANSI_RESET);
        }
    }
    /**
     * Lists contents of a directory in the DFS.
     *
     * @throws IOException If directory listing fails
     */
    private void listDirectory() throws IOException {
        String path = lineReader.readLine("Enter path to list (or press Enter for root): ");
        path = path.isEmpty() ? "/" : normalizePath(path);

        List<FileMetaData> files = dirOps.listDirectory(path);

        if (files.isEmpty()) {
            System.out.println(ANSI_YELLOW + "Directory is empty" + ANSI_RESET);
            return;
        }

        // Display directory contents in formatted table
        System.out.println("\n" + ANSI_BLUE +
                String.format("%-40s %-8s %12s %-20s", "Name", "Type", "Size", "Last Modified") +
                ANSI_RESET);
        System.out.println("-".repeat(80));

        for (FileMetaData file : files) {
            System.out.printf("%-40s %-8s %12s %-20s%n",
                    file.getName(),
                    file.isDirectory() ? "DIR" : "FILE",
                    formatFileSize(file.getSize()),
                    dateFormat.format(file.getLastModified()));
        }
    }

    /**
     * Formats file size in human-readable format.
     *
     * @param size File size in bytes
     * @return Formatted string representation of the file size
     */
    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    /**
     * Displays a progress bar for ongoing operations.
     *
     * @param current Current progress value
     * @param total Total expected value
     * @param operation Name of the operation being performed
     */
    private void showProgressBar(long current, long total, String operation) {
        int width = 50;
        int progress = (int) ((current * width) / total);
        System.out.print("\r" + operation + " [");
        for (int i = 0; i < width; i++) {
            System.out.print(i < progress ? "=" : " ");
        }
        System.out.printf("] %.1f%%", (current * 100.0) / total);
    }

    private void showReplicationStatus() {
        try(Socket socket = new Socket(serverAddress, serverPort);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Read the file path from the user
            String filePath = lineReader.readLine("Enter file path to show replication status: ");
            String normalizedPath = normalizePath(filePath);

            // Create the command object
            Command command = new Command(Command.Type.SHOW_REPLICATION_STATUS, normalizedPath);
            oos.writeObject(command);
            oos.flush();

            // Read te server's response
            Object response = ois.readObject();
            if(response instanceof Command) {
                System.out.println(ANSI_BLUE + "Replication status: " + response.toString() + ANSI_RESET);
            } else {
                System.out.println(ANSI_YELLOW + "Unexpected response from server." + ANSI_RESET);
            }

        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error showing replication: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void showNodeHealth() {
        try(Socket socket = new Socket(serverAddress, serverPort);
           ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
           ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.SHOW_NODE_HEALTH);
            oos.writeObject(command);
            oos.flush();

            Object response = ois.readObject();

            if(response instanceof Command) {
                System.out.println(ANSI_BLUE + "Node health: " + response.toString() + ANSI_RESET);
            } else {
                System.out.println(ANSI_YELLOW + "Unexpected response from server." + ANSI_RESET);
            }
        } catch(Exception e) {
            System.out.println(ANSI_YELLOW + "Error reading node health: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void forceReplication() {
        try(Socket socket = new Socket(serverAddress, serverPort);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            // Read the file path from the user
            String filePath = lineReader.readLine("Enter file path to force replication: ");
            String normalizedPath = normalizePath(filePath);

            // Create the command object
            Command command = new Command(Command.Type.FORCE_REPLICATION, normalizedPath);
            oos.writeObject(command);
            oos.flush();

            // Read the server's response
            Object response = ois.readObject();
            if(response instanceof Command) {
                System.out.println(ANSI_BLUE + "Replication status: " + response.toString() + ANSI_RESET);
            } else {
                System.out.println(ANSI_YELLOW + "Unexpected response from server." + ANSI_RESET);
            }
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error  forcing replication: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * Displays the main menu of the DFS client.
     */
    private void showPrompt() {
        System.out.println("\n" + ANSI_GREEN + "╔════════════════════════════════════╗");
        System.out.println("║     Distributed File System        ║");
        System.out.println("╠════════════════════════════════════-╣");
        System.out.println("║ 1. List Directory                  ║");
        System.out.println("║ 2. Create Directory                ║");
        System.out.println("║ 3. Delete Directory                ║");
        System.out.println("║ 4. Upload File                     ║");
        System.out.println("║ 5. Download File                   ║");
        System.out.println("║ 6. Move/Rename File/Directory      ║");
        System.out.println("║ 7. Create Version                  ║");
        System.out.println("║ 8. List Versions                   ║");
        System.out.println("║ 9. Restore Version                 ║");
        System.out.println("║ 10. Update File and Create Version ║");
        System.out.println("║ 11. Show Replication Status        ║");
        System.out.println("║ 12. Show Node Health               ║");
        System.out.println("║ 13. Force Replication              ║");
        System.out.println("║ 0. Exit                            ║");
        System.out.println("╚════════════════════════════════════╝" + ANSI_RESET);
    }

    /**
     * Starts the DFS client interface and handles user commands.
     */

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
                        String localPath = lineReader.readLine("Enter local file Path: ");
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
                    case "7" -> createVersion();
                    case "8" -> listVersions();
                    case "9" -> restoreVersion();
                    case "10" -> updateFile();
                    case "11" -> showReplicationStatus();
                    case "12" -> showNodeHealth();
                    case "13" -> forceReplication();
                    case "0" -> {
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
    private void monitorNodeHealth() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                showNodeHealth();
            } catch (Exception e) {
                LOGGER.warning("Failed to check node health: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
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