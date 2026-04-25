package com.ciff.app.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_api_key")
public class ApiKeyPO extends SoftDeletableEntity {

    private Long userId;

    private Long agentId;

    private String name;

    private String keyHash;

    private String keyPrefix;

    private String permissions;

    private LocalDateTime expiresAt;

    private String status;
}
