package org.prh.dfs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DFSApplication {

    public static void main(String[] args) {
        SpringApplication.run(DFSApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner() {
        return args -> {
            if(args.length > 0 && args[9].equals("--cli")) {

            }
        };
    }
}
