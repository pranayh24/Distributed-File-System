package org.prh.dfs.server;

import org.prh.dfs.fault.HeartbeatMonitor;
import org.prh.dfs.integration.DFSIntegrator;
import org.prh.dfs.replication.ReplicationManager;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Main server class for the Distributed File System.
 * Handles incoming connections and delegates them to ServerHandler instances.
 */
public class FileServer {
    private static final Logger LOGGER = Logger.getLogger(FileServer.class.getName());
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 60;

    private final DFSIntegrator dfsIntegrator;
    private final ReplicationManager replicationManager;
    private final HeartbeatMonitor heartbeatMonitor;

    private final int port;
    private final String storagePath;
    private final ExecutorService executorService;
    private ServerSocket serverSocket;
    private boolean running = true;
    private final String nodeId;

    public FileServer(int port, String storagePath) {
        this.port = port;
        this.storagePath = storagePath;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        this.dfsIntegrator = new DFSIntegrator();
        this.replicationManager = dfsIntegrator.getReplicationManager();
        this.heartbeatMonitor = dfsIntegrator.getHeartbeatMonitor();

        this.nodeId = dfsIntegrator.addNode("localhost", port);

        initializeLogging();
        initializeStorageDirectory();
        initializeReplication();
    }

    private void initializeLogging() {
        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File("logs");
            if(!logsDir.exists()) {
                logsDir.mkdirs();
            }

            // Setup file handler for logging
            FileHandler fileHandler = new FileHandler("logs/fileserver_%g.log", 524880, 5, true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.INFO);

        } catch(Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
        }
    }

    private void initializeStorageDirectory() {
        File storageDir = new File(storagePath);
        if(!storageDir.exists()) {
            if(storageDir.mkdirs()) {
                LOGGER.info("Storage directory created: " + storagePath);
            } else {
                throw new RuntimeException("Failed to create storage directory: " + storagePath);
            }
        }
        LOGGER.info("Storage directory initialized at:" + storagePath);
    }

    public void initializeReplication() {
        dfsIntegrator.initializeSystem();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            dfsIntegrator.shutdown();
            stop();
        }));


    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("File server started on port: " + port);

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            while(running) {
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("Client connected: " + clientSocket.getInetAddress());

                    // Submit client handling task to thread pool
                    executorService.submit(new ServerHandler(clientSocket, storagePath, dfsIntegrator));
                } catch (Exception e) {
                    if(running) {
                        LOGGER.log(Level.SEVERE, "Error handling client request", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting server" ,e);
        } finally {
            stop();
        }
    }

    public void stop() {
        if(!running) {
            return;
        }

        running = false;
        LOGGER.info("Initiating server shutdown...");

        try {
            // Close the server socket
            if(serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                LOGGER.info("Server socket closed");
            }

            // Shutdown the executor service
            executorService.shutdown();
            if(!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                LOGGER.warning("Forced shutdown of thread pool after timeout");
            } else {
                LOGGER.info("Thread pool shutdown completed gracefully");
            }
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE,"Error during server shutdown", e);
            executorService.shutdownNow();
        }

        LOGGER.info("Server stopped");
    }

    public static void main(String[] args) {
        int port = 8888;
        String storagePath = "D:\\DFSStorage\\";

        try {
            FileServer fileServer = new FileServer(port, storagePath);
            fileServer.start();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error in main", e);
            System.exit(1);
        }
    }
}
