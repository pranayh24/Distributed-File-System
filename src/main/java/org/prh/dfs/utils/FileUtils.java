package org.prh.dfs.utils;

import java.io.File;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FileUtils {
    public static final int DEFAULT_CHUNK_SIZE = 1024*1024;
    private static final ConcurrentHashMap<String, TransferProgress> progressTrackers = new ConcurrentHashMap<String, TransferProgress>();

    public static class TransferProgress {
        private final AtomicLong totalChunks;
        private final AtomicLong processedChunks;
        private final long totalSize;

        public TransferProgress(long totalChunks, long totalSize) {
            this.totalChunks = new AtomicLong(totalChunks);
            this.processedChunks = new AtomicLong(0);
            this.totalSize = totalSize;
        }

        public void incrementProcessedChunks() {
            processedChunks.incrementAndGet();
        }

        public double getProgress() {
            return (double) processedChunks.get() / totalChunks.get() * 100;
        }
    }

    public static String calculateCheckSum(byte[] data) {
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for(byte b: hash) {
                String hex = Integer.toString(0xff & b);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }

    public static String initializeTransfer(File file, int chunkSize) {
        String transferId = UUID.randomUUID().toString();
        long totalChunks = (file.length() + chunkSize -1) / chunkSize;
        progressTrackers.put(transferId, new TransferProgress(totalChunks, file.length()));
        return transferId;
    }

    public static TransferProgress getProgress(String transferId) {
        return progressTrackers.get(transferId);
    }

    public static void removeProgress(String transferId) {
        progressTrackers.remove(transferId);
    }
}
