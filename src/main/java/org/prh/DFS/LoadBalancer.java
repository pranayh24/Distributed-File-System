package org.prh.DFS;

import java.util.ArrayList;
import java.util.List;

public class LoadBalancer {

    private final List<String> servers; // List of server addresses
    private int currentIndex; // Index of the current server

    public LoadBalancer() {
        this.servers = new ArrayList<>();
        this.currentIndex = 0;
    }

    // Register a new Server
    public void registerServer(String serverAddress) {
        servers.add(serverAddress);
        System.out.println("Server registered: " + serverAddress);
    }

    // Remove a server
    public void removeServer(String serverAddress) {
        if(servers.remove(serverAddress)) {
            System.out.println("Server removed: " + serverAddress);
        } else {
            System.out.println("Server not found: " + serverAddress);
        }
    }

    // Get the next server (Round-robin)
    public String getNextServer() {
        if(servers.isEmpty()) {
            throw new IllegalStateException("No servers available");
        }

        String nextServer = servers.get(currentIndex);
        currentIndex = (currentIndex + 1) % servers.size(); // Round-robin
        return nextServer;
    }

    // List all servers
    public List<String> listServers() {
        return new ArrayList<>(servers);
    }
}
