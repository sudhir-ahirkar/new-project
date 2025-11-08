package com.toll.edge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // Required for @Scheduled to work
public class EdgeApplication {
    public static void main(String[] args) { SpringApplication.run(EdgeApplication.class, args); }
}
