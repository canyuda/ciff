package com.ciff.app.dto.apikey;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApiKeyVO {

    private Long id;
    private String name;
    private String keyPrefix;
    private Long agentId;
    private String status;
    private LocalDateTime expiresAt;
    private LocalDateTime createTime;

    private String rawKey;
}
