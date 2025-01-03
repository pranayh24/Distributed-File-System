package org.prh.dfs.server;

import org.prh.dfs.model.*;
import org.prh.dfs.utils.FileUtils;
import org.prh.dfs.versioning.VersionManager;

import java.io.*;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Combined handler for both file operations and directory commands in the distributed file system.
 * Handles file chunk processing and directory operations in a thread-safe manner.
 */
public class ServerHandler implements Runnable{
    private static final Logger LOGGER = Logger.getLogger(ServerHandler.class.getName());

    private final Socket clientSocket;
    private final String storagePath;
    private final DirectoryHandler directoryHandler;
    private final VersionManager versionManager;
    private FileOperationResult result;

    // Thread-safe maps for file operations
    private static final ConcurrentHashMap<String, FileOutputStream> activeFiles = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String,Object> fileLocks = new ConcurrentHashMap<>();

    public ServerHandler(Socket clientSocket, String storagePath) {
        this.clientSocket = clientSocket;
        this.storagePath = storagePath;
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

            if(request instanceof Command) {
                handleCommand((Command) request, oos);
            } else if(request instanceof FileChunk) {
                handleFileChunk((FileChunk) request, oos);
            } else {
                throw new IllegalArgumentException("Unknown request type: " + request.getClass());
            }
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling chunk: ", e);
        } finally {
            closeClientSocket();
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
                    List<FileMetaData> files = directoryHandler.listDirectory(command.getPath());
                    oos.writeObject(files);
                    break;

                case CREATE_DIR:
                    boolean created = directoryHandler.createDirectory(command.getPath());
                    oos.writeObject(created);
                    break;

                case DELETE_DIR:
                    boolean deleted = directoryHandler.deleteDirectory(command.getPath());
                    oos.writeObject(deleted);
                    break;

                case MOVE:
                case RENAME:
                    boolean moved = directoryHandler.moveOrRename(command.getPath(), command.getNewPath());
                    oos.writeObject(moved);
                    break;

                case UPLOAD_FILE:
                    //
                    oos.writeObject(true);
                    break;

                case DOWNLOAD_FILE:
                    //
                    handleFileDownload(command.getPath(), oos);
                    break;

                case DELETE_FILE:
                    boolean fileDeleted = new File(storagePath, command.getPath()).delete();
                    oos.writeObject(fileDeleted);
                    break;

                case CREATE_VERSION:
                    try {
                        Version version = versionManager.createVersion(
                                command.getPath(),
                                command.getCreator(),
                                command.getComment()
                        );
                        result = FileOperationResult.success("Version created successfully", version);
                        oos.writeObject(result);
                        oos.flush();
                    } catch(Exception e) {
                        LOGGER.severe("Error creating version: " + e.getMessage());
                        result = FileOperationResult.error("Failed to create version: " + e.getMessage());
                        oos.writeObject(result);
                        oos.flush();
                    }
                    break;

                case LIST_VERSIONS:
                    try {
                        String normalizedPath = command.getPath().replace("^/+","");
                        List<Version> versions = versionManager.getVersions(normalizedPath);
                        result = FileOperationResult.success("Versions retrieved successfully", versions);
                        LOGGER.info("Found " + versions.size() + " versions for " + normalizedPath);
                        oos.writeObject(result);
                        oos.flush();
                    } catch (Exception e) {
                        LOGGER.severe("Error listing versions: " + e.getMessage());
                        oos.writeObject(FileOperationResult.error(e.getMessage()));
                        oos.flush();
                    }
                    break;

                case RESTORE_VERSION:
                    try {
                        versionManager.restoreVersion(command.getPath(), command.getVersionId());
                        result = FileOperationResult.success("Version restored successfully");
                        oos.writeObject(result);
                        oos.flush();
                    } catch(Exception e) {
                        LOGGER.severe("Error restoring version: " + e.getMessage());
                        oos.writeObject(FileOperationResult.error(e.getMessage()));
                        oos.flush();
                    }
                    break;

                default:
                    result = FileOperationResult.error("Unsupported command type: " + command.getType());
                    oos.writeObject(result);
                    oos.flush();
            }
        } catch(Exception e) {
            LOGGER.severe("Error processing command: " + e.getMessage());
            oos.writeObject("Error: " + e.getMessage()); // Indicate failure to the client
            oos.flush();
        }
    }

    private List<FileMetaData> listDirectory(String path) throws IOException {
        Path dirPath = Paths.get(storagePath, path);
        List<FileMetaData> files = new ArrayList<>();

        if (!Files.exists(dirPath)) {
            return files; // Return empty list for non-existent directory
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                files.add(new FileMetaData(
                        entry.getFileName().toString(),
                        entry.toString().substring(storagePath.length()).replace('\\', '/'),
                        Files.isDirectory(entry),
                        Files.size(entry),
                        new Date(Files.getLastModifiedTime(entry).toMillis())
                ));
            }
        }
        return files;
    }

    /**
     * Handles incoming file chunks for file upload.
     * @param chunk
     * @param oos
     * @throws IOException
     */
    private void handleFileChunk(FileChunk chunk, ObjectOutputStream oos) throws IOException {
        LOGGER.info(() -> String.format("Received chunk %d for file: %s",
                chunk.getChunkNumber(), chunk.getFileName()));

        if(!validateCheckSum(chunk)) {
            oos.writeObject("CHECKSUM_MISMATCH");
            return;
        }

        processChunk(chunk);
        oos.writeObject("CHUNK_RECEIVED");
    }

    /**
     * Handles file download requests.
     * @param path
     * @param oos
     * @throws IOException
     */
    private void handleFileDownload(String path, ObjectOutputStream oos) throws IOException {
        File file = new File(storagePath, path);
        if(!file.exists() || !file.isFile()) {
            oos.writeObject(null);
            return;
        }

        try(FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while((bytesRead = fis.read(buffer)) != -1) {
                oos.write(buffer, 0, bytesRead);
            }
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

    /**
     * Processes a received file chunk.
     * @param chunk
     * @throws IOException
     */
    private void processChunk(FileChunk chunk) throws IOException {
        String fileId = chunk.getFileId();
        Object fileLock = fileLocks.computeIfAbsent(fileId, k -> new Object());

        synchronized (fileLock) {
            //Normalize the filename by replacing any path  separators with the system's separator
            String normalizedFileName = chunk.getFileName().replace('/', File.separatorChar);
            // Remove any duplicate file name in the path
            if(normalizedFileName.contains(File.separator)) {
                normalizedFileName = normalizedFileName.substring(normalizedFileName.lastIndexOf(File.separator) + 1);
            }
            String fullPath = storagePath + File.separator + normalizedFileName;
            LOGGER.info(() -> "Processing chunk to file: " + fullPath);


            FileOutputStream fos = getOrCreateOutputStream(fileId, fullPath);
            writeChunkData(chunk, fos);

            if (isLastChunk(chunk)) {
                closeAndCleanUp(fileId, chunk.getFileName(), fos);
            }
        }
    }

    private FileOutputStream getOrCreateOutputStream(String fileId, String fullPath) {
        return activeFiles.computeIfAbsent(fileId, k -> {
            try {
                return new FileOutputStream(fullPath);
            } catch(FileNotFoundException e) {
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

    private void closeClientSocket() {
        try {
            clientSocket.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing client socket", e);
        }
    }
}
