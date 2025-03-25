package org.pr.dfs.server;

import org.pr.dfs.model.Node;
import org.pr.dfs.model.ReplicationStatus;
import org.pr.dfs.replication.FaultToleranceManager;
import org.pr.dfs.replication.NodeManager;
import org.pr.dfs.replication.ReplicationManager;
import org.pr.dfs.utils.MetricsCollector;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private final ScheduledExecutorService scheduledExecutorService;
    private final MetricsCollector metricsCollector;
    private final Node thisNode;
    private ServerSocket serverSocket;
    private boolean running = true;

    public FileServer(int port, String storagePath) {
        this.port = port;
        this.storagePath = storagePath;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(3);
        this.metricsCollector = new MetricsCollector();

        // Initialize this node
        this.thisNode = initializeThisNode();

        // Initialize managers
        this.nodeManager = new NodeManager();
        this.replicationManager = new ReplicationManager(DEFAULT_REPLICATION_FACTOR, nodeManager);
        this.faultToleranceManager = new FaultToleranceManager(nodeManager, replicationManager);

        // Register this node with the node manager
        this.nodeManager.registerNode(thisNode);

        initializeLogging();
        initializeStorageDirectory();
        startScheduledTasks();
    }

    private Node initializeThisNode() {
        try {
            String address = InetAddress.getLocalHost().getHostAddress();
            Node node = new Node(address, port);
            node.setHealthy(true);
            node.setLastHeartbeat(System.currentTimeMillis());
            node.setAvailableDiskSpace(getDiskSpace());
            return node;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize server node", e);
            throw new RuntimeException("Failed to initialize server node", e);
        }
    }

    private long getDiskSpace() {
        try {
            Path path = Paths.get(storagePath);
            // Create directory first if it doesn't exist
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            return Files.getFileStore(path).getUsableSpace();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get disk space", e);
            return 0;
        }
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

            LOGGER.info("Logging initialized for DFS server - " + thisNode.getNodeId());
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

        // Create version control directory if it doesn't exist
        File versionDir = new File(storagePath, ".versions");
        if(!versionDir.exists() && !versionDir.mkdirs()) {
            LOGGER.warning("Failed to create version control directory");
        }

        // Create metadata directory if it doesn't exist
        File metaDataDir = new File(storagePath, ".metadata");
        if(!metaDataDir.exists() && !metaDataDir.mkdirs()) {
            LOGGER.warning("Failed to create metadata directory");
        }
    }

    private void startScheduledTasks() {
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                List<Node> nodes = nodeManager.getAllNodes();
                int totalNodes = nodes.size();
                int healthyNodes = 0;

                LOGGER.info("Running scheduled health check for " + totalNodes + " nodes");

                for(Node node : nodes) {
                    boolean wasHealthy = node.isHealthy();
                    nodeManager.checkNodeHealth(node);

                    if(node.isHealthy()) {
                        healthyNodes++;
                    } else if(wasHealthy) {
                        // Node just became unhealthy
                        LOGGER.warning("Node " + node.getNodeId() + " is not responding");
                        faultToleranceManager.onNodeFailure(node.getNodeId());
                    }
                }

                // Update this node's health status
                thisNode.setLastHeartbeat(System.currentTimeMillis());
                thisNode.setAvailableDiskSpace(getDiskSpace());

                LOGGER.info("Health check complete: " + healthyNodes + "/" + totalNodes +  " nodes healthy");

                // Update metrics
                metricsCollector.recordNodeStats(healthyNodes, totalNodes - healthyNodes);
            } catch(Exception e) {
                LOGGER.log(Level.SEVERE, "Error during health check", e);
            }
        }, 0, HEALTH_CHECK_INTERVAL, TimeUnit.MILLISECONDS);

        // Schedule metrics collection
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("Collecting System metrics");
                metricsCollector.collectSystemMetrics();
                metricsCollector.logMetricsSummary();
            } catch(Exception e) {
                LOGGER.log(Level.SEVERE, "Error collecting metrics", e);
            }
        }, METRICS_COLLECTION_INTERVAL, METRICS_COLLECTION_INTERVAL, TimeUnit.MILLISECONDS);

        // Schedule periodic recovery check
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                LOGGER.info("Running periodic recovery check");
                faultToleranceManager.checkAndRecover();
            } catch(Exception e) {
                LOGGER.log(Level.SEVERE, "Error during periodIC recovery check", e);
            }
        }, RECOVERY_CHECK_INTERVAL, RECOVERY_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }


    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            LOGGER.info("DFS server started on port: " + port);
            LOGGER.info("Node ID: " + thisNode.getNodeId());
            LOGGER.info("Storage Path: " + storagePath);
            LOGGER.info("Replication factor: " + DEFAULT_REPLICATION_FACTOR);

            System.out.println("DFS server started on port " + port);
            System.out.println("Node ID: " + thisNode.getNodeId());

            // Update this node's status
            thisNode.setStartTime(System.currentTimeMillis());

            while(running) {
                try {
                    // Accept incoming connections
                    Socket clientSocket = serverSocket.accept();
                    LOGGER.info("New client connected: " + clientSocket.getInetAddress());

                    executorService.submit(new ServerHandler(clientSocket, storagePath,
                            nodeManager, replicationManager, faultToleranceManager));

                    metricsCollector.incrementConnectionCount();
                } catch (Exception e) {
                    if(running) {
                        LOGGER.log(Level.SEVERE, "Error handling client request", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting server" ,e);
            throw new RuntimeException("Failed to start server", e);
        } finally {
            shutdown();
        }
    }


    public void shutdown() {
        running = false;

        LOGGER.info("Shutting down DFS server...");

        // Close server socket
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                LOGGER.info("Server socket closed");
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error closing server socket", e);
            }
        }

        // Shutdown executors
        LOGGER.info("Shutting down executor services");
        executorService.shutdown();
        scheduledExecutorService.shutdown();

        try {
            if (!executorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledExecutorService.awaitTermination(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                scheduledExecutorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Notify other nodes about shutdown
        try {
            nodeManager.notifyNodeShutdown(thisNode.getNodeId());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error notifying about shutdown", e);
        }

        LOGGER.info("DFS server shutdown complete");
    }

    public ReplicationStatus getReplicationStatus(String filePath) {
        return replicationManager.getReplicationStatus(filePath);
    }

    public NodeManager getNodeManager() {
        return nodeManager;
    }

    public ReplicationManager getReplicationManager() {
        return replicationManager;
    }

    public Node getThisNode() {
        return thisNode;
    }

    public static void main(String[] args) {
        int port = 8888;
        String storagePath = "D:\\dfs_storage\\";

        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default: 8888");
            }
        }

        if (args.length > 1) {
            storagePath = args[1];
        }

        // Start the server
        FileServer server = new FileServer(port, storagePath);
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.start();
    }
}
