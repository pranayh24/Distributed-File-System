package org.pr.dfs.node;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SimpleHTTPNodeServer {

    private final String nodeId;
    private final int port;
    private final Path storagePath;
    private HttpServer server;

    public SimpleHTTPNodeServer(String nodeId, int port) {
        this.nodeId = nodeId;
        this.port = port;
        this.storagePath = Paths.get("./storage", nodeId);
    }

    public void start() throws IOException {
        // Create storage directory
        Files.createDirectories(storagePath);

        // Create HTTP server
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Add endpoints
        server.createContext("/node/health", new HealthHandler());
        server.createContext("/node/files", new FileHandler());
        server.createContext("/node/info", new InfoHandler());

        // Start server
        server.setExecutor(null);
        server.start();

        System.out.println("✅ Simple Node Server started: " + nodeId + " on port " + port);
        System.out.println("📁 Storage path: " + storagePath.toAbsolutePath());
        System.out.println("🔗 Health check: http://localhost:" + port + "/node/health");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("❌ Node server stopped: " + nodeId);
        }
    }

    // Health check endpoint
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = String.format(
                    "{\"nodeId\":\"%s\",\"status\":\"healthy\",\"port\":%d,\"timestamp\":%d,\"storageAvailable\":%d}",
                    nodeId, port, System.currentTimeMillis(), getAvailableStorage()
                );

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                System.out.println("✅ Health check: " + nodeId);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // File operations endpoint
    private class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String query = exchange.getRequestURI().getQuery();

            if ("POST".equals(method)) {
                // Store file
                handleFileUpload(exchange, query);
            } else if ("GET".equals(method)) {
                // Get file
                handleFileDownload(exchange, query);
            } else if ("DELETE".equals(method)) {
                // Delete file
                handleFileDelete(exchange, query);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }

        private void handleFileUpload(HttpExchange exchange, String query) throws IOException {
            // Parse filePath from query
            String filePath = getQueryParam(query, "filePath");
            if (filePath == null) {
                sendError(exchange, 400, "Missing filePath parameter");
                return;
            }

            // URL decode the file path to handle spaces and special characters
            try {
                filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
            } catch (Exception e) {
                sendError(exchange, 400, "Invalid filePath encoding");
                return;
            }

            // Read file data from request body
            byte[] fileData = exchange.getRequestBody().readAllBytes();

            // Save file
            Path targetPath = storagePath.resolve(filePath);
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, fileData);

            String response = String.format(
                "{\"success\":true,\"nodeId\":\"%s\",\"filePath\":\"%s\",\"size\":%d}",
                nodeId, filePath, fileData.length
            );

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            System.out.println("📁 File stored: " + filePath + " (" + fileData.length + " bytes)");
        }

        private void handleFileDownload(HttpExchange exchange, String query) throws IOException {
            String filePath = getQueryParam(query, "filePath");
            if (filePath == null) {
                sendError(exchange, 400, "Missing filePath parameter");
                return;
            }

            // URL decode the file path to handle spaces and special characters
            try {
                filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
            } catch (Exception e) {
                sendError(exchange, 400, "Invalid filePath encoding");
                return;
            }

            Path targetPath = storagePath.resolve(filePath);
            if (!Files.exists(targetPath)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] fileData = Files.readAllBytes(targetPath);
            exchange.sendResponseHeaders(200, fileData.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileData);
            }

            System.out.println("📥 File retrieved: " + filePath);
        }

        private void handleFileDelete(HttpExchange exchange, String query) throws IOException {
            String filePath = getQueryParam(query, "filePath");
            if (filePath == null) {
                sendError(exchange, 400, "Missing filePath parameter");
                return;
            }

            // URL decode the file path to handle spaces and special characters
            try {
                filePath = java.net.URLDecoder.decode(filePath, "UTF-8");
            } catch (Exception e) {
                sendError(exchange, 400, "Invalid filePath encoding");
                return;
            }

            Path targetPath = storagePath.resolve(filePath);
            boolean deleted = Files.deleteIfExists(targetPath);

            String response = String.format(
                "{\"success\":%b,\"nodeId\":\"%s\",\"filePath\":\"%s\"}",
                deleted, nodeId, filePath
            );

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes().length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }

            System.out.println("🗑️ File deleted: " + filePath + " (success: " + deleted + ")");
        }
    }

    // Node info endpoint
    private class InfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                long fileCount = 0;
                try {
                    fileCount = Files.walk(storagePath)
                            .filter(Files::isRegularFile)
                            .count();
                } catch (Exception e) {
                    // Ignore
                }

                String response = String.format(
                    "{\"nodeId\":\"%s\",\"port\":%d,\"storagePath\":\"%s\",\"availableStorage\":%d,\"hostedFiles\":%d,\"uptime\":%d}",
                    nodeId, port, storagePath.toString(), getAvailableStorage(), fileCount, System.currentTimeMillis()
                );

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        String response = String.format("{\"success\":false,\"error\":\"%s\"}", message);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, response.getBytes().length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private String getQueryParam(String query, String param) {
        if (query == null) return null;

        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && param.equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }

    private long getAvailableStorage() {
        try {
            return Files.getFileStore(storagePath).getUsableSpace();
        } catch (Exception e) {
            return -1;
        }
    }

    public static void main(String[] args) {
        String nodeId = System.getProperty("node.id", "simple_node_" + System.currentTimeMillis());
        int port = Integer.parseInt(System.getProperty("server.port", "8091"));

        SimpleHTTPNodeServer server = new SimpleHTTPNodeServer(nodeId, port);

        try {
            server.start();

            // Keep running
            System.out.println("Press Enter to stop the server...");
            System.in.read();

            server.stop();

        } catch (IOException e) {
            System.err.println("❌ Server error: " + e.getMessage());
        }
    }
}
