package org.prh.DFS.server;

import org.prh.DFS.model.FileChunk;
import org.prh.DFS.utils.FileUtils;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkHandler implements Runnable{
    private final Socket clientSocket;
    private final String storagePath;
    private static final ConcurrentHashMap<String, FileOutputStream> activeFiles = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String,Object> fileLocks = new ConcurrentHashMap<>();

    public ChunkHandler(Socket clientSocket, String storagePath) {
        this.clientSocket = clientSocket;
        this.storagePath = storagePath;
    }

    @Override
    public void run(){
        try(ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {

            FileChunk chunk = (FileChunk) ois.readObject();

            // Verify checksum
            String calculatedChecksum = FileUtils.calculateCheckSum(chunk.getData());
            if(!calculatedChecksum.equals(chunk.getChecksum())) {
                oos.writeObject("CHECKSUM_MISMATCH");
                return;
            }

            // Process the chunk
            processChunk(chunk);
            oos.writeObject("CHUNK_RECEIVED");

        } catch(Exception e) {
            System.err.println("Error handling chunk: " + e.getMessage());
        }
    }

    private void processChunk(FileChunk chunk) throws IOException {
        String fileId = chunk.getFileId();
        Object fileLock = fileLocks.computeIfAbsent(fileId, k-> new Object());

        synchronized (fileLock) {
            FileOutputStream fos = activeFiles.computeIfAbsent(fileId, k -> {
                try {
                    return new FileOutputStream(storagePath + File.separator + chunk.getFileName());
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

            // Write the chunk data
            fos.write(chunk.getData());
            fos.flush();

            // If this is the last chunk, close the file
            if(chunk.getChunkNumber() == chunk.getTotalChunks() - 1) {
                fos.close();
                activeFiles.remove(fileId);
                fileLocks.remove(fileId);
            }
        }
    }
}
