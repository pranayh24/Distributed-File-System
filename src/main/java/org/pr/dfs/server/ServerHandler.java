package org.pr.dfs.server;

import org.pr.dfs.model.*;
import org.pr.dfs.replication.*;
import org.pr.dfs.utils.FileUtils;
import org.pr.dfs.versioning.VersionManager;

import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Combined handler for both file operations and directory commands in the distributed file system.
 * Handles file chunk processing and directory operations in a thread-safe manner.
 */
public class ServerHandler implements Runnable{
    private static final Logger LOGGER = Logger.getLogger(ServerHandler.class.getName());
    private static final int REPLICATION_TIMEOUT = 30000; // 30 seconds
    private static final int MAX_RETRIES = 3;
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private static final long MAX_FILE_SIZE = 1024L * 1024L * 1024L; // 1GB max file size

    private final Socket clientSocket;
    private final String storagePath;
    private final NodeManager nodeManager;
    private final ReplicationManager replicationManager;
    private final FaultToleranceManager faultToleranceManager;
    private final DirectoryHandler directoryHandler;
    private final VersionManager versionManager;
    private FileOperationResult result;

    // Thread-safe maps for file operations
    private static final ConcurrentHashMap<String, FileOutputStream> activeFiles = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String,Object> fileLocks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReplicationStatus> replicationStatuses = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Set<String>> fileNodeMap = new ConcurrentHashMap<>();

    public ServerHandler(Socket clientSocket, String storagePath,
                         NodeManager nodeManager, ReplicationManager replicationManager, FaultToleranceManager faultToleranceManager) {
        this.clientSocket = clientSocket;
        this.storagePath = storagePath;
        this.nodeManager = nodeManager;
        this.replicationManager = replicationManager;
        this.faultToleranceManager = faultToleranceManager;
        this.directoryHandler = new DirectoryHandler(storagePath);
        this.versionManager = new VersionManager(storagePath);
        LOGGER.info(() -> "Created new ServerHandler for client: " + clientSocket.getInetAddress());
    }

    @Override
    public void run(){
        try(ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {

            //Read the first object to determine the type of operation
            Object request = ois.readObject();
            processRequest(request, oos);

        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling chunk: ", e);
        } finally {
            closeClientSocket();
        }
    }

    private void processRequest(Object request, ObjectOutputStream oos) throws IOException {
        if(request instanceof Command) {
            handleCommand((Command) request, oos);
        } else if(request instanceof FileChunk) {
            handleFileChunk((FileChunk) request, oos);
        } else {
            throw new IllegalArgumentException("Unknown request type: " + request.getClass());
        }
    }
    /**
     * Handles directory-related commands.
     * @param command
     * @param oos
     * @throws IOException
     */
    private void handleCommand(Command command, ObjectOutputStream oos) throws  IOException {
        LOGGER.info(() -> "Handling command: " + command.getType());

        try {
            switch (command.getType()) {
                case LIST_DIR:
                    handleListDirectory(command.getPath(), oos);
                    break;

                case CREATE_DIR:
                    handleCreateDirectory(command.getPath(), oos);
                    break;

                case DELETE_DIR:
                    handleDeleteDirectory(command.getPath(), oos);
                    break;

                case MOVE:
                case RENAME:
                    handleMoveOrRename(command, oos);
                    break;

                case UPLOAD_FILE:
                    handleFileUpload(command.getPath(), oos);
                    break;

                case DOWNLOAD_FILE:
                    handleFileDownload(command.getPath(), oos);
                    break;

                case DELETE_FILE:
                    handleFileDelete(command.getPath(), oos);
                    break;

                case CREATE_VERSION:
                    handleCreateVersion(command, oos);
                    break;

                case LIST_VERSIONS:
                    handleListVersions(command.getPath(), oos);
                    break;

                case RESTORE_VERSION:
                    handleRestoreVersion(command, oos);

                case SHOW_REPLICATION_STATUS:
                    handleShowReplicationStatus(command.getPath(), oos);
                    break;

                case FORCE_REPLICATION:
                    handleForceReplication(command.getPath(), oos);
                    break;

                case SHOW_NODE_HEALTH:
                    handleShowNodeHealth(oos);
                    break;

                default:
                    sendError(oos, "Unsupported command type: " + command.getType());
            }
        } catch(Exception e) {
            sendError(oos, "Error processing command: " + e.getMessage());
        }
    }


    /**
     * Handles incoming file chunks for file upload.
     * @param chunk
     * @param oos
     * @throws IOException
     */
    private void handleFileChunk(FileChunk chunk, ObjectOutputStream oos) throws IOException {
        String filePath = chunk.getFileName();

        try{
            if(!validateChunk(chunk)) {
                sendError(oos, "Invalid chunk received");
                return;
            }

            boolean processed = processChunkWithRetry(chunk);

            if(processed && isLastChunk(chunk)) {
                CompletableFuture<Boolean> replicationFuture = startReplication(filePath);
                updateReplicationStatus(filePath, replicationFuture);
                sendSuccess(oos, "File successfully processed and replication initiated");
            } else if (processed) {
                sendSuccess(oos, "Chunk processed successfully");
            } else {
                sendError(oos, "Failed to process chunk");
            }
        } catch(Exception e) {
            LOGGER.severe("Error handling file chunk: " + e.getMessage());
            sendError(oos, "Error processing chunk: " + e.getMessage());
        }
    }

    private boolean validateChunk(FileChunk chunk) {
        if(chunk.getData() == null || chunk.getData().length == 0) {
            LOGGER.warning("Empty chunk received");
            return false;
        }

        if(!validateCheckSum(chunk)) {
            String fullPath = getFullPath(chunk.getFileName());
            File file = new File(fullPath);

            // Check if total file size would exceed limit
            long currentSize = file.exists() ? file.length() : 0;
            long newSize = currentSize + chunk.getData().length;
            if(newSize > MAX_FILE_SIZE) {
                LOGGER.warning("File size would exceed maximum limit");
                return false;
            }
        }
        return true;
    }

    private boolean processChunkWithRetry(FileChunk chunk) throws IOException {
        int attempts = 0;
        while(attempts < MAX_RETRIES) {
            try {
                processChunk(chunk);
                return true;
            } catch(IOException e) {
                attempts++;
                LOGGER.warning(String.format("Attempt %d failed for chunk %d of %s: %s", attempts, chunk.getChunkNumber(), chunk.getFileName(), e.getMessage()));

                if(attempts == MAX_RETRIES) {
                    LOGGER.severe("Max retry attempts reached for chunk processing");
                    return false;
                }

                try{
                    Thread.sleep(1000 * attempts);
                } catch(InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Processes a received file chunk.
     * @param chunk
     * @throws IOException
     */
    private void processChunk(FileChunk chunk) throws IOException {
        String fileId = chunk.getFileId();
        Object fileLock = fileLocks.computeIfAbsent(fileId, k -> new Object());

        synchronized (fileLock) {
            String fullPath = getFullPath(chunk.getFileName());
            FileOutputStream fos = getOrCreateOutputStream(fileId, fullPath);

            try {
                writeChunkData(chunk, fos);

                if(isLastChunk(chunk)) {
                    closeAndCleanUp(fileId, chunk.getFileName(), fos);
                }
            } catch(IOException e) {
                handleChunkProcessingError(fileId, fos, e);
                throw e;
            }
        }
    }

    private CompletableFuture<Boolean> startReplication(String filePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                replicationManager.replicateFile(filePath);
                return true;
            } catch (Exception e) {
                LOGGER.severe("Replication failed for " +filePath + ": " + e.getMessage());
                return false;
            }
        })
        .orTimeout(REPLICATION_TIMEOUT, TimeUnit.SECONDS)
        .exceptionally(ex -> {
            LOGGER.severe("Replication failed with timeout or error: " + ex.getMessage());
            return false;
        });
    }

    private void updateReplicationStatus(String filePath, CompletableFuture<Boolean> replicationFuture) {
        ReplicationStatus status = new ReplicationStatus(filePath);
        replicationStatuses.put(filePath, status);

        replicationFuture.thenAccept(success -> {
            status.setStatus(success ? ReplicationStatus.Status.COMPLETED : ReplicationStatus.Status.FAILED);
            if(success) {
                updateFileNodeMapping(filePath);
            }
        });
    }

    private void updateFileNodeMapping(String filePath) {
        Set<String> nodes = fileNodeMap.computeIfAbsent(filePath, k -> ConcurrentHashMap.newKeySet());
        nodeManager.getHealthyNodes().forEach(node -> nodes.add(node.getNodeId()));
    }

    private void handleListDirectory(String path, ObjectOutputStream oos) throws IOException {
        try {
            List<FileMetaData> files = directoryHandler.listDirectory(path);
            sendSuccess(oos, "Directory listed successfully", files);
        } catch(Exception e) {
            sendError(oos, "Failed to list directory: " + e.getMessage());
        }
    }

    private void handleCreateDirectory(String path, ObjectOutputStream oos) throws IOException {
        try {
            boolean created = directoryHandler.createDirectory(path);
            if(created) {
                sendSuccess(oos, "Directory created successfully");
            } else {
                sendError(oos, "Failed to create directory");
            }
        } catch(Exception e) {
            sendError(oos, "Error creating directory: " + e.getMessage());
        }
    }

    private void handleDeleteDirectory(String path, ObjectOutputStream oos) throws IOException {
        try {
            boolean deleted = directoryHandler.deleteDirectory(path);
            if(deleted) {
                removeReplicationStatus(path);
                sendSuccess(oos, "Directory deleted successfully");
            } else {
                sendError(oos, "Failed to delete directory");
            }
        } catch(Exception e) {
            sendError(oos, "Error deleting directory: " + e.getMessage());
        }
    }

    private void handleMoveOrRename(Command command, ObjectOutputStream oos) throws IOException {
        try {
            boolean moved = directoryHandler.moveOrRename(command.getPath(), command.getNewPath());
            if(moved) {
                updateReplicationStatusAfterMove(command.getPath(), command.getNewPath());
                sendSuccess(oos, "File moved/renamed successfully");
            } else {
                sendError(oos, "Failed to move/rename file");
            }
        } catch (Exception e) {
            sendError(oos, "Error moving/renaming file: " + e.getMessage());
        }
    }

    private void handleFileUpload(String path, ObjectOutputStream oos) throws IOException {
        try {
            validateFilePath(path);
            sendSuccess(oos, "Ready to receive file chunks");
        } catch(Exception e) {
            sendError(oos, "Error preparing for file upload: " + e.getMessage());
        }
    }

    /**
     * Handles file download requests.
     * @param path
     * @param oos
     * @throws IOException
     */
    private void handleFileDownload(String path, ObjectOutputStream oos) throws IOException {
        Path filePath = Paths.get(storagePath, path);

        if(!Files.exists(filePath)) {
            sendError(oos, "File not found: " + filePath);
            return;
        }

        try {
            sendFileInChunks(filePath, oos);
        } catch(Exception e) {
            sendError(oos, "Error downloading file: " + e.getMessage());
        }
    }

    private void handleFileDelete(String path, ObjectOutputStream oos) throws IOException {
        try {
            Path filePath = Paths.get(storagePath, path);
            boolean deleted = Files.deleteIfExists(filePath);

            if(deleted) {
                removeReplicationStatus(path);
                replicationManager.handleFileDeletion(path);
                sendSuccess(oos, "File deleted successfully");
            } else {
                sendError(oos, "File not found");
            }
        } catch(Exception e) {
            sendError(oos, "Error deleting file: " + e.getMessage());
        }
    }

    private void handleCreateVersion(Command command, ObjectOutputStream oos) throws IOException {
        try {
            Version version = versionManager.createVersion(
                    command.getPath(),
                    command.getCreator(),
                    command.getComment()
            );
            sendSuccess(oos, "Version created successfully", version);
        } catch(Exception e) {
            sendError(oos, "Error creating version: " + e.getMessage());
        }
    }

    private void handleListVersions(String path, ObjectOutputStream oos) throws IOException {
        try {
            List<Version> versions = versionManager.getVersions(path);
            sendSuccess(oos, "Version retrieved successfully", versions);
        } catch (Exception e) {
            sendError(oos, "Error listing versions: " + e.getMessage());
        }
    }

    private void handleRestoreVersion(Command command, ObjectOutputStream oos) throws IOException {
        try {
            versionManager.restoreVersion(command.getPath(), command.getVersionId());
            sendSuccess(oos, "Version restored successfully");
        } catch(Exception e) {
            sendError(oos, "Error restoring version: " + e.getMessage());
        }
    }

    private void handleShowReplicationStatus(String path, ObjectOutputStream oos) throws IOException {
        ReplicationStatus status = replicationStatuses.get(path);
        if(status != null) {
            sendSuccess(oos, "Replication status retrieved", status);
        } else {
            sendError(oos, "No replication status found for path: " + path);
        }
    }

    private void handleForceReplication(String path, ObjectOutputStream oos) throws IOException {
        try{
            CompletableFuture<Boolean> replicableFuture = startReplication(path);
            updateReplicationStatus(path, replicableFuture);
            sendSuccess(oos, "Force Replication initiated");
        } catch(Exception e) {
            sendError(oos, "Force replication failed: " + e.getMessage());
        }
    }

    private void handleShowNodeHealth(ObjectOutputStream oos) throws IOException {
        try {
            List<Node> nodes = nodeManager.getHealthyNodes();
            Map<String, NodeHealthInfo> healthStatus = new HashMap<>();

            for(Node node : nodes) {
                NodeHealthInfo info = new NodeHealthInfo(
                        node.isHealthy(),
                        node.getLastHeartbeat(),
                        fileNodeMap.values().stream()
                                .filter(nodeSet -> nodeSet.contains(node.getNodeId()))
                                .count()
                );
                healthStatus.put(node.getNodeId(), info);
            }

            sendSuccess(oos, "Node health status retrieved", healthStatus);
        } catch (Exception e) {
            sendError(oos, "Error retrieving node health: " + e.getMessage());
        }
    }

    private String getFullPath(String fileName) {
        return Paths.get(storagePath, fileName).toString();
    }

    private void validateFilePath(String path) throws IOException {
        if(path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path normalizedPath = Paths.get(storagePath, path).normalize();
        if(!normalizedPath.startsWith(storagePath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
    }

    /**
     * Validates the checksum of a received chunk.
     * @param chunk
     * @return
     */
    private boolean validateCheckSum(FileChunk chunk) {
        String calculatedChecksum = FileUtils.calculateCheckSum((chunk.getData()));
        return calculatedChecksum.equals(chunk.getChecksum());
    }

    private boolean saveChunkLocally(FileChunk chunk) {
        LOGGER.info(() -> String.format("Received chunk %d for file: %s",
                chunk.getChunkNumber(), chunk.getFileName()));

        if(!validateCheckSum(chunk)) {
            return false;
        }
        try {
            processChunk(chunk);
            return true;
        } catch (IOException e) {
            LOGGER.severe("Failed to save chunk locally: " + e.getMessage());
            return false;
        }

    }

    private FileOutputStream getOrCreateOutputStream(String fileId, String fullPath) {
        return activeFiles.computeIfAbsent(fileId, k -> {
            try {
                Files.createDirectories(Paths.get(fullPath).getParent());
                return new FileOutputStream(fullPath);
            } catch(IOException e) {
                throw new RuntimeException("Failed to create output stream for file: " + fullPath, e);
            }
        });
    }

    private void writeChunkData(FileChunk chunk, FileOutputStream fos) throws IOException {
        fos.write(chunk.getData());
        fos.flush();
    }

    private boolean isLastChunk(FileChunk chunk) {
        return chunk.getChunkNumber() == chunk.getTotalChunks() - 1;
    }

    private void closeAndCleanUp(String fileId, String fileName, FileOutputStream fos) throws IOException {
        fos.close();
        activeFiles.remove(fileId);
        fileLocks.remove(fileId);
        LOGGER.info( () -> "File completed: " + fileName);
    }

    private void handleChunkProcessingError(String fileId, FileOutputStream fos, IOException e) {
        try {
            fos.close();
        } catch(IOException ioe) {
            LOGGER.warning("Error closing file stream: " + ioe.getMessage());
        }
        activeFiles.remove(fileId);
        fileLocks.remove(fileId);
    }

    private void removeReplicationStatus(String path) {
        replicationStatuses.remove(path);
        fileNodeMap.remove(path);
    }

    private void updateReplicationStatusAfterMove(String oldPath, String newPath) {
        ReplicationStatus status = replicationStatuses.get(oldPath);
        if(status != null) {
            status.updatePath(newPath);
            replicationStatuses.put(newPath, status);
        }

        Set<String> nodes = fileNodeMap.remove(oldPath);
        if (nodes != null) {
            fileNodeMap.put(newPath, nodes);
        }
    }

    private void sendFileInChunks(Path filePath, ObjectOutputStream oos) throws IOException {
        long fileSize = Files.size(filePath);
        long totalChunks = fileSize / CHUNK_SIZE;
        String fileId = UUID.randomUUID().toString();

        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int chunkNumber = 0;
            int bytesRead;

            while((bytesRead = fis.read(buffer)) != -1) {
                byte[] chunkData = bytesRead < CHUNK_SIZE ?
                        Arrays.copyOf(buffer, bytesRead) : buffer;

                FileChunk chunk = new FileChunk(
                        fileId,
                        filePath.getFileName().toString(),
                        chunkNumber++,
                        chunkData,
                        FileUtils.calculateCheckSum(chunkData),
                        totalChunks
                );

                oos.writeObject(chunk);
                oos.flush();
            }
        }
    }

    private void sendSuccess(ObjectOutputStream oos, String message) throws IOException {
        sendSuccess(oos, message, null);
    }

    private void sendSuccess(ObjectOutputStream oos, String message, Object data) throws IOException {
        FileOperationResult result = new FileOperationResult(false, message, data);
        oos.writeObject(result);
        oos.flush();
    }

    private void sendError(ObjectOutputStream oos, String message) throws IOException {
        FileOperationResult result = new FileOperationResult(false, message);
        oos.writeObject(result);
        oos.flush();
    }

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing client socket", e);
        }
    }

    private static class ReplicationStatus implements Serializable {
        private static final long serialVersionUID = 1L;

        enum Status {
            PENDING,
            IN_PROGRESS,
            COMPLETED,
            FAILED
        }

        private String filePath;
        private Status status;
        private final long startTime;
        private long completionTime;
        private List<String> replicatedNodes;

        public ReplicationStatus(String filePath) {
            this.filePath = filePath;
            this.status = Status.PENDING;
            this.startTime = System.currentTimeMillis();
            this.replicatedNodes = new ArrayList<>();
        }

        public void setStatus(Status status) {
            this.status = status;
            if(status == Status.COMPLETED || status == Status.FAILED) {
                this.completionTime = System.currentTimeMillis();
            }
        }

        public void updatePath(String newPath) {
            this.filePath = newPath;
        }

        public void addReplicatedNode(String nodeId) {
            this.replicatedNodes.add(nodeId);
        }
    }

    private static class NodeHealthInfo implements Serializable {
        private static final long serialVersionUID = 1L;

        private final boolean healthy;
        private final long lastHeartbeat;
        private final long hostedFilesCount;

        public NodeHealthInfo(boolean healthy, long lastHeartbeat, long hostedFilesCount) {
            this.healthy = healthy;
            this.lastHeartbeat = lastHeartbeat;
            this.hostedFilesCount = hostedFilesCount;
        }
    }
}
