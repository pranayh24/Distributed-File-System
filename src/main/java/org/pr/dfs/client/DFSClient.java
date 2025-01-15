package org.pr.dfs.client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.pr.dfs.model.*;
import org.pr.dfs.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class DFSClient {
    private static final Logger LOGGER = Logger.getLogger(DFSClient.class.getName());

    private static final int CHUNK_SIZE = 1024 * 1024;
    private static final String PATH_SEPARATOR = "/";
    private static final int BUFFER_SIZE = 8192;

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_YELLOW = "\u001B[33m";

    private final String serverAddress;
    private final int serverPort;
    private final DirectoryOperations dirOps;
    private final FileOperations fileOps;
    private final VersionOperations versionOps;
    private final ReplicationMonitor replicationMonitor;
    private final SimpleDateFormat dateFormat;
    private final LineReader lineReader;
    private final ScheduledExecutorService scheduler;

    public DFSClient(String serverAddress, int serverPort) throws IOException {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.dirOps = new DirectoryOperations(serverAddress, serverPort);
        this.fileOps = new FileOperations(serverAddress, serverPort);
        this.versionOps = new VersionOperations(serverAddress, serverPort);
        this.replicationMonitor = new ReplicationMonitor(serverAddress, serverPort);
        this.dateFormat = new SimpleDateFormat("dd MMM HH:mm");

        Terminal terminal = TerminalBuilder.builder().system(true).build();
        this.lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String cleanPath = path.replaceAll("^\"|\"$", "");
        if (path.contains(":")) {
            return Paths.get(cleanPath).toString();
        }
        return cleanPath.replace("\\", "/")
                .replaceAll("/+", "/")
                .replaceAll("/$", "");
    }

    public void uploadFile(String localPath, String remotePath) throws IOException {
        String normalizedLocalPath = Paths.get(normalizePath(localPath)).toString();
        String normalizedRemotePath = normalizePath(remotePath);

        File file = new File(normalizedLocalPath);
        if (!file.exists()) {
            throw new FileNotFoundException("Local file not found: " + normalizedLocalPath);
        }

        String fileName = file.getName();
        String fullRemotePath = normalizedRemotePath.endsWith(PATH_SEPARATOR) ?
                normalizedRemotePath + fileName : normalizedRemotePath + PATH_SEPARATOR + fileName;

        long fileSize = file.length();
        int totalChunks = (int) Math.ceil(fileSize / (double) CHUNK_SIZE);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            int chunkNumber = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunkData = bytesRead < CHUNK_SIZE ? new byte[bytesRead] : buffer;
                if (bytesRead < CHUNK_SIZE) {
                    System.arraycopy(buffer, 0, chunkData, 0, bytesRead);
                }

                FileChunk chunk = new FileChunk(
                        UUID.randomUUID().toString(),
                        fileName,
                        chunkNumber++,
                        chunkData,
                        FileUtils.calculateCheckSum(chunkData),
                        totalChunks
                );

                sendChunk(chunk);
                showProgressBar(chunkNumber, totalChunks, "Uploading");
            }
        }
    }

    private void sendChunk(FileChunk chunk) throws IOException {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {
            oos.writeObject(chunk);
            oos.flush();
        }
    }

    public void downloadFile(String remotePath, String localPath) throws IOException {
        String normalizedRemotePath = normalizePath(remotePath);
        String normalizedLocalPath = Paths.get(normalizePath(localPath)).toString();

        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.DOWNLOAD_FILE, normalizedRemotePath);
            oos.writeObject(command);
            oos.flush();

            try (FileOutputStream fos = new FileOutputStream(normalizedLocalPath)) {
                FileChunk chunk;
                while ((chunk = (FileChunk) ois.readObject()) != null) {
                    fos.write(chunk.getData());
                    showProgressBar(chunk.getChunkNumber(), chunk.getTotalChunks(), "Downloading");
                }
            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to download file: " + e.getMessage(), e);
            }
        }
    }

    private void createVersion() {
        try {
            String path = lineReader.readLine("Enter file path: ");
            String creator = lineReader.readLine("Enter your name: ");
            String comment = lineReader.readLine("Enter version comment: ");

            Command command = new Command(Command.Type.CREATE_VERSION, normalizePath(path), null, creator, comment);
            versionOps.createVersion(command);
            System.out.println(ANSI_GREEN + "Version created successfully" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error creating version: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void listVersions() {
        try {
            String path = lineReader.readLine("Enter file path: ");
            List<Version> versions = versionOps.listVersions(normalizePath(path));
            versions.forEach(version -> System.out.println(version));
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error listing versions: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void restoreVersion() {
        try {
            String path = lineReader.readLine("Enter file path: ");
            String versionId = lineReader.readLine("Enter version ID: ");
            versionOps.restoreVersion(normalizePath(path), versionId);
            System.out.println(ANSI_GREEN + "Version restored successfully" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "Error restoring version: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void showReplicationStatus() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.SHOW_REPLICATION_STATUS);
            oos.writeObject(command);
            oos.flush();

            String status = (String) ois.readObject();
            System.out.println(ANSI_BLUE + "Replication Status: " + ANSI_RESET + status);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(ANSI_YELLOW + "Error showing replication status: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void showNodeHealth() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.SHOW_NODE_HEALTH);
            oos.writeObject(command);
            oos.flush();

            String healthStatus = (String) ois.readObject();
            System.out.println(ANSI_BLUE + "Node Health Status: " + ANSI_RESET + healthStatus);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(ANSI_YELLOW + "Error showing node health: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void forceReplication() {
        try (Socket socket = new Socket(serverAddress, serverPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {

            Command command = new Command(Command.Type.FORCE_REPLICATION);
            oos.writeObject(command);
            oos.flush();
            String response = (String) ois.readObject();
            System.out.println(ANSI_BLUE + "Force Replication Response: " + ANSI_RESET + response);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(ANSI_YELLOW + "Error forcing replication: " + e.getMessage() + ANSI_RESET);
        }
    }

    private void showMenu() {
        System.out.println(ANSI_BLUE + "\n╔════════════════════════════════════╗");
        System.out.println("║        DFS Client Interface         ║");
        System.out.println("╠════════════════════════════════════╣");
        System.out.println("║ File Operations:                    ║");
        System.out.println("║ 1. List Directory                   ║");
        System.out.println("║ 2. Create Directory                 ║");
        System.out.println("║ 3. Delete Directory                 ║");
        System.out.println("║ 4. Upload File                      ║");
        System.out.println("║ 5. Download File                    ║");
        System.out.println("║ 6. Move/Rename File                 ║");
        System.out.println("║                                     ║");
        System.out.println("║ Version Control:                    ║");
        System.out.println("║ 7. Create Version                   ║");
        System.out.println("║ 8. List Versions                    ║");
        System.out.println("║ 9. Restore Version                  ║");
        System.out.println("║                                     ║");
        System.out.println("║ System Status:                      ║");
        System.out.println("║ 10. Show Node Health                ║");
        System.out.println("║ 11. Show Replication Status         ║");
        System.out.println("║ 12. Force Replication               ║");
        System.out.println("║                                     ║");
        System.out.println("║ 0. Exit                             ║");
        System.out.println("╚════════════════════════════════════╝" + ANSI_RESET);
    }

    public void start() {
        while (true) {
            try {
                showMenu();
                String choice = lineReader.readLine("Choose an option: ");

                switch (choice) {
                    case "1" -> listDirectory();
                    case "2" -> createDirectory();
                    case "3" -> deleteDirectory();
                    case "4" -> uploadFile();
                    case "5" -> downloadFile();
                    case "6" -> moveOrRenameFile();
                    case "7" -> createVersion();
                    case "8" -> listVersions();
                    case "9" -> restoreVersion();
                    case "10" -> showNodeHealth();
                    case "11" -> showReplicationStatus();
                    case "12" -> forceReplication();
                    case "0" -> {
                        cleanup();
                        return;
                    }
                    default -> System.out.println(ANSI_YELLOW + "Invalid option, please try again" + ANSI_RESET);
                }
            } catch (IOException e) {
                LOGGER.warning("Error in client operation: " + e.getMessage());
                System.out.println(ANSI_YELLOW + "Operation failed: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    private void cleanup() {
        System.out.println(ANSI_GREEN + "Shutting down DFS client..." + ANSI_RESET);
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        System.out.println(ANSI_GREEN + "Goodbye!" + ANSI_RESET);
    }

    public static void main(String[] args) {
        try {
            DFSClient client = new DFSClient("localhost", 8888);
            client.start();
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            System.exit(1);
        }
    }
}