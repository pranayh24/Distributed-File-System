package org.pr.dfs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dfs")
public class DfsConfig {
    private Storage storage = new Storage();
    private Server server = new Server();
    private Replication replication = new Replication();

    @Data
    public static class Storage {
        private String path = "./dfs_storage";
    }

    @Data
    public static class Server {
        private String host = "localhost";
        private int port = 8888;
    }

    @Data
    public static class Replication {
        private int factor = 3;
    }
}
