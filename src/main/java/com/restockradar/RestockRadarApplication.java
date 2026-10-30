package com.restockradar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RestockRadarApplication {

    private static final Logger log = LoggerFactory.getLogger(RestockRadarApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RestockRadarApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("\n\u001B[32m==================================================================\u001B[0m");
        System.out.println("\u001B[32m  🚀 RestockRadar Application is running successfully!           \u001B[0m");
        System.out.println("\u001B[32m  👉 Access the application at: \u001B[36m\u001B[4mhttp://localhost:8080\u001B[0m     ");
        System.out.println("\u001B[32m==================================================================\n\u001B[0m");
    }
}
