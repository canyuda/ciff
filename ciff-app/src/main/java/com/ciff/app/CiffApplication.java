package com.ciff.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.ciff")
@EnableScheduling
public class CiffApplication {

    public static void main(String[] args) {
        SpringApplication.run(CiffApplication.class, args);
    }
}