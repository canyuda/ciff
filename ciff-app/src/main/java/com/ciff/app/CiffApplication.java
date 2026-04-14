package com.ciff.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.ciff")
public class CiffApplication {

    public static void main(String[] args) {
        SpringApplication.run(CiffApplication.class, args);
    }
}