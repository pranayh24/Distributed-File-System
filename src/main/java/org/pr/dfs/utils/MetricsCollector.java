package org.pr.dfs.utils;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public class MetricsCollector {
    private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

    // Connection metrics
    private final AtomicInteger connectionCount = new AtomicInteger(0);
    private final AtomicInteger activeConnectionCount = new AtomicInteger(0);

    // File operation metrics
    private final AtomicInteger fileUploadCount = new AtomicInteger(0);
    private final AtomicInteger fileDownloadCount = new AtomicInteger(0);
    private final AtomicLong bytesUploaded = new AtomicLong(0);
    private final AtomicLong bytesDownloaded = new AtomicLong(0);

    // Replication metrics
    private final AtomicInteger replicationSuccessCount = new AtomicInteger(0);
    private final AtomicInteger replicationFailureCount = new AtomicInteger(0);

    // Node metrics
    private final AtomicInteger healthyNodeCount = new AtomicInteger(0);
    private final AtomicInteger unhealthyNodeCount = new AtomicInteger(0);

    // System metrics
    private long totalDiskSpace = 0;
    private long usableDiskSpace = 0;
    private double systemLoadAverage = 0;
    private long freeMemory = 0;
    private long totalMemory = 0;

    // Start time
    private final long startTime = System.currentTimeMillis();

    public void recordNodeStats(int healthy, int unhealthy) {
        healthyNodeCount.set(healthy);
        unhealthyNodeCount.set(unhealthy);
    }

    public void incrementConnectionCount() {
        connectionCount.incrementAndGet();
        activeConnectionCount.incrementAndGet();
    }

    public void decrementConnectionCount() {
        activeConnectionCount.decrementAndGet();
    }

    public void recordFileUpload(long bytes) {
        fileUploadCount.incrementAndGet();
        bytesUploaded.addAndGet(bytes);
    }

    public void recordFileDownload(long bytes) {
        fileDownloadCount.incrementAndGet();
        bytesDownloaded.addAndGet(bytes);
    }

    public void recordReplicationSuccess() {
        replicationSuccessCount.incrementAndGet();
    }

    public void recordReplicationFailure() {
        replicationFailureCount.incrementAndGet();
    }

    public void collectSystemMetrics() {
        try {
            // Disk metrics
            Path path = Paths.get(".");
            FileStore fileStore = Files.getFileStore(path);
            totalDiskSpace = fileStore.getTotalSpace();
            usableDiskSpace = fileStore.getUsableSpace();

            // Memory metrics
            Runtime runtime = Runtime.getRuntime();
            freeMemory = runtime.freeMemory();
            totalMemory = runtime.totalMemory();

            // CPU metrics
            systemLoadAverage = runtime.availableProcessors();

        } catch(IOException e) {
            LOGGER.warning("Failed to collect system metrics" +
                    ": " + e.getMessage());
        }
    }

    public void logMetricsSummary() {
        long uptime = System.currentTimeMillis() - startTime;
        long upTimeMinutes = uptime / (60* 1000);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== DFS Server Metrics ===\n");
        sb.append("Uptime: ").append(upTimeMinutes).append(" minutes\n");
        sb.append("Connections: ").append(connectionCount.get())
                .append(" (active: ").append(activeConnectionCount.get()).append(")\n");

        sb.append("File Operations: ")
                .append(fileUploadCount.get()).append(" uploads, ")
                .append(fileDownloadCount.get()).append(" downloads\n");

        sb.append("Data Transferred: ")
                .append(formatBytes(bytesUploaded.get())).append(" up, ")
                .append(formatBytes(bytesDownloaded.get())).append(" down\n");

        sb.append("Replication: ")
                .append(replicationSuccessCount.get()).append(" successful, ")
                .append(replicationFailureCount.get()).append(" failed\n");

        sb.append("Nodes: ")
                .append(healthyNodeCount.get()).append(" healthy, ")
                .append(unhealthyNodeCount.get()).append(" unhealthy\n");

        sb.append("Disk Space: ")
                .append(formatBytes(usableDiskSpace)).append(" free of ")
                .append(formatBytes(totalDiskSpace)).append("\n");

        sb.append("Memory: ")
                .append(formatBytes(freeMemory)).append(" free of ")
                .append(formatBytes(totalMemory)).append(" allocated");

        LOGGER.info(sb.toString());
    }

    private String formatBytes(long bytes) {
        if(bytes < 1024) return bytes + " B";
        if(bytes < 1024 *1024) return String.format("%.1f KB", bytes / 1024);
        if(bytes < 1024*1024*1024) return String.format("%.1f MB", bytes / (1024*1024));
        return String.format("%.1f GB", bytes / (1024*1024*1024));
    }

}
