package com.ciff.app.dto.apikey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ApiKeyCreateRequest {

    @NotBlank(message = "name is required")
    @Size(min = 1, max = 128, message = "name must be 1-128 characters")
    private String name;

    @NotNull(message = "agentId is required")
    private Long agentId;

    private OffsetDateTime expiresAt;
}
