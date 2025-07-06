package org.pr.dfs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DFSApplication {

    public static void main(String[] args) {
        SpringApplication.run(DFSApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            System.out.println("DFS Rest API Server Started!");
            System.out.println("API Documentation: http://localhost:8080/api/swagger-ui.html");
            System.out.println("API Endpoints: http://localhost:8080/api/api-docs");

            if(args.length > 0 && args[0].equals("-h")) {
                System.out.println("CLI mode not implemented in this version, run main server and client classes to access the CLI mode.");
            }
        };
    }
}
