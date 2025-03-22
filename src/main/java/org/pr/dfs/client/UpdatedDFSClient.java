/**package org.pr.dfs.client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.pr.dfs.model.FileMetaData;
import org.pr.dfs.model.FileOperationResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;

public class UpdatedDFSClient {
    private static final Logger LOGGER = Logger.getLogger(UpdatedDFSClient.class.getName());
    private static final int CHUNK_SIZE = 1024*1024; // 1 MB chunks
    private static final String PATH_SEPARATOR = "/";
    private static final int HEALTH_CHECK_INTERVAL = 30;

    private final String serverAddress;
    private final int serverPort;
    private final DFSClientCommunicator communicator;
    private final DirectoryOperations dirops;
    private final VersionOperations verops;
    private final SimpleDateFormat dateFormat;
    private final LineReader lineReader;
    private final ScheduledExecutorService scheduler;

    public UpdatedDFSClient(String serverAddress, int serverPort) throws IOException {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.communicator = new DFSClientCommunicator(serverAddress, serverPort);
        this.dirops = new DirectoryOperations(communicator);
        this.verops = new VersionOperations(communicator);
        this.dateFormat = new SimpleDateFormat("dd MMM HH:mm");

        Terminal terminal = TerminalBuilder.builder().system(true).build();
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .completer(new StringsCompleter("ls", "cd", "mkdir", "upload", "download", "mv", "cp",
                        "version-create", "version-list", "version-restore", "version-update",
                        "replication-status", "node-health", "replication-force", "exit"))
                .build();

        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    private void handleCommand(String line) throws IOException, ClassNotFoundException {
        String[] parts = line.split("\\s+");
        String command = parts[0];

        switch(command) {
            case "ls":
                listDirectory(parts.length > 1 ? parts[1] : "/");
                break;

            case "cd":
                break;

            case "mkdir":
                createDirectory(parts[1]);
                break;

            case "rmdir":
                deleteDirectory(parts[1]);
                break;

            case "upload":
                uploadFile(parts[1], parts[2]);
                break;

            case "download":
                downloadFile(parts[1], parts[2]);
                break;

            case "mv":
            case "cp":
                moveOrRename(parts[1], parts[2]);
                break;

            case "version-create":
                createVersion(parts[1], parts[2]);
                break;

            case "version-list":
                listVersions(parts[1]);
                break;

            case "version-restore":
                restoreVersion(parts[1], parts[2]);
                break;

            case "version-update":
                updateFile(parts[1], parts[2], parts[3]); //Local Path, remote Path, comment
                break;

            case "replication-status":
                showReplicationStatus(parts[1]);
                break;

            case "node-health":
                showNodeHealth();
                break;

            case "replication-force":
                forceReplication(parts[1]);
                break;
            case "exit":
                System.out.println("Goodbye!");
                System.exit(0);
                break;

            default:
                System.out.println("Invalid command: " + command);
        }
    }

    private void listDirectory(String path) throws IOException, ClassNotFoundException {
        FileOperationResult result = (FileOperationResult) dirops.listDirectory(path);

        if(result.isSuccess()) {
            List<FileMetaData> files = (List<FileMetaData>) result.getData();

            if (files.isEmpty()) {
                System.out.println("Directory is empty");
                return;
            }

            // Display directory contents in formatted table
            System.out.println("\n" +
                    String.format("%-40s %-8s %12s %-20s", "Name", "Type", "Size", "Last Modified")
                    );
            System.out.println("-".repeat(80));

            for (FileMetaData file : files) {
                System.out.printf("%-40s %-8s %12s %-20s%n",
                        file.getName(),
                        file.isDirectory() ? "DIR" : "FILE",
                        formatFileSize(file.getSize()),
                        dateFormat.format(file.getLastModified()));
            }
        } else {
            System.out.println(result.getMessage());
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + "B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private void createDirectory(String path) throws IOException, ClassNotFoundException {
        FileOperationResult result = FileOperationResult.success(dirops.createDirectory(path));
        if (result.isSuccess()) {
            System.out.println("Directory created successfully");
        } else {
            System.out.println(result.getMessage());
        }
    }

    private void deleteDirectory(String path) throws IOException, ClassNotFoundException {
        FileOperationResult result = FileOperationResult.success(dirops.deleteDirectory(path));
        if(result.isSuccess()) {
            System.out.println("Directory deleted successfully");
        } else {
            System.out.println(result.getMessage());
        }
    }

    private void uploadFile(String localPath, String remotePath) throws FileNotFoundException {
        File file = new File(localPath);
        if(!file.exists()) {
            throw new FileNotFoundException("Local file not found: " + localPath);
        }
        String fileName = file.getName();
        String fullRemotePath = remotePath.endsWith(PATH_SEPARATOR) ?
                remotePath + fileName : remotePath + PATH_SEPARATOR + fileName;

        long fileSize = file.length();
        int totalChunks = (int) Math.ceil(fileSize / (double) CHUNK_SIZE);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int chunkNumber = 0;

            while(true) {
                int bytesRead = fis.read(buffer);
                if(bytesRead == -1) break;

                byte[] chunkData = (bytesRead < CHUNK_SIZE) ?
                        new byte[bytesRead] : buffer.clone();

                if (bytesRead < CHUNK_SIZE) {
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                }
            }
        } catch (IOException e) {

        }
    }



}*/