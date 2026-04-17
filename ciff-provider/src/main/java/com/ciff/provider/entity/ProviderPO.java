package com.ciff.provider.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.common.entity.SoftDeletableEntity;
import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderStatus;
import com.ciff.common.enums.ProviderType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import com.ciff.provider.dto.ProviderAuthConfig;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_provider", autoResultMap = true)
public class ProviderPO extends SoftDeletableEntity {

    private String name;

    private ProviderType type;

    private AuthType authType;

    private String apiBaseUrl;

    private String apiKeyEncrypted;

    private ProviderStatus status;

    @TableField(value = "auth_config", typeHandler = JacksonTypeHandler.class)
    private ProviderAuthConfig authConfig;
}
