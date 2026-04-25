package com.ciff.app.config;

import com.ciff.app.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Value("${ciff.admin.default-username:admin}")
    private String defaultAdminUsername;

    @Value("${ciff.admin.default-password:admin123}")
    private String defaultAdminPassword;

    @Override
    public void run(String... args) {
        if (!userService.usernameExists(defaultAdminUsername)) {
            userService.createUser(defaultAdminUsername, defaultAdminPassword, "admin");
            log.info("Default admin user created: {}", defaultAdminUsername);
        }
    }
}
