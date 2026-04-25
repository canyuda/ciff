package com.ciff.app.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "username is required")
    @Size(min = 2, max = 64, message = "username must be 2-64 characters")
    private String username;

    @NotBlank(message = "password is required")
    @Size(min = 6, max = 128, message = "password must be 6-128 characters")
    private String password;
}
