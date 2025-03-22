package org.pr.dfs.server;

import org.pr.dfs.model.Node;
import org.pr.dfs.replication.FaultToleranceManager;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.replication.ReplicationManager;

import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
    private static final int DEFAULT_REPLICATION_FACTOR = 3;
    private static final long HEALTH_CHECK_INTERVAL = 30000; // 30 seconds
    private static final long METRICS_COLLECTION_INTERVAL = 60000; // 60 seconds
    private static final long RECOVERY_CHECK_INTERVAL = 300000; // 5 minutes

    private final int port;
    private final String storagePath;
    private final ExecutorService executorService;
    private final NodeManager nodeManager;
    private final ReplicationManager replicationManager;
    private final FaultToleranceManager faultToleranceManager;
    private final ScheduledExecutorService healthCheckExecutor;
    private ServerSocket serverSocket;
    private boolean running = true;

    public FileServer(int port, String storagePath) {
        this.port = port;
        this.storagePath = storagePath;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.healthCheckExecutor = Executors.newScheduledThreadPool(1);

        this.nodeManager = new NodeManager();
        this.replicationManager = new ReplicationManager(DEFAULT_REPLICATION_FACTOR, nodeManager);
        this.faultToleranceManager = new FaultToleranceManager(nodeManager, replicationManager);
        initializeLogging();
        initializeStorageDirectory();
        startHealthCheck();
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

    private void startHealthCheck() {
        healthCheckExecutor.scheduleAtFixedRate(() -> {
            try{
                List<Node> nodes = nodeManager.getHealthyNodes();
                for(Node node : nodes) {
                    if(!node.isHealthy()) {
                        LOGGER.warning("Node " + node.getNodeId() + " is not healthy");
                        faultToleranceManager.onNodeFailure(node.getNodeId());
                    }
                }
            } catch(Exception e){
                LOGGER.log(Level.SEVERE, "Error starting health check", e);
            }
        }, HEALTH_CHECK_INTERVAL, HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("File server started on port: " + port);

            //Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

            while(running) {
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("Client connected: " + clientSocket.getInetAddress());

                    ServerHandler serverHandler = new ServerHandler(clientSocket, storagePath,
                            nodeManager, replicationManager, faultToleranceManager);
                    executorService.execute(serverHandler);
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
