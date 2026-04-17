package com.ciff.provider.dto;

import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新供应商请求")
public class ProviderUpdateRequest {

    @Size(max = 100, message = "名称最长100个字符")
    @Schema(description = "供应商名称", example = "OpenAI")
    private String name;

    @Schema(description = "供应商类型", example = "openai")
    private ProviderType type;

    @Schema(description = "认证类型", example = "bearer")
    private AuthType authType;

    @Schema(description = "API 基础地址", example = "https://api.openai.com")
    private String apiBaseUrl;

    @Schema(description = "API Key（明文，传入则重新加密）", example = "sk-proj-xxx")
    private String apiKey;

    @Schema(description = "认证扩展配置")
    private ProviderAuthConfig authConfig;

    @Schema(description = "状态", example = "active")
    private String status;
}
