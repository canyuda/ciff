package com.ciff.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "供应商响应")
public class ProviderVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "供应商名称")
    private String name;

    @Schema(description = "供应商类型", example = "openai")
    private String type;

    @Schema(description = "供应商类型显示名", example = "OpenAI")
    private String typeDisplayName;

    @Schema(description = "认证类型", example = "bearer")
    private String authType;

    @Schema(description = "API 基础地址")
    private String apiBaseUrl;

    @Schema(description = "API Key 掩码", example = "sk-pr***")
    private String apiKeyMasked;

    @Schema(description = "认证扩展配置")
    private ProviderAuthConfig authConfig;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "关联模型列表")
    private List<ModelVO> models;

    @Schema(description = "健康状态")
    private ProviderHealthVO health;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
