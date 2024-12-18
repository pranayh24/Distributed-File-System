package org.prh.DFS;

public class Main {

    public static void main(String[] args) {
        // register servers
        LoadBalancer loadBalancer = new LoadBalancer();

        loadBalancer.registerServer("Server1");
        loadBalancer.registerServer("Server2");
        loadBalancer.registerServer("Server3");

        // Simulate requests
        for(int i=0;i<10;i++) {
            String server = loadBalancer.getNextServer();
            System.out.println("Request sent to: " + server);
        }

        // Remove a server
        loadBalancer.removeServer("Server3");

        // Simulate requests
        for(int i=0;i<5;i++) {
            String server = loadBalancer.getNextServer();
            System.out.println("Request sent to: " + server);
        }
    }

}
