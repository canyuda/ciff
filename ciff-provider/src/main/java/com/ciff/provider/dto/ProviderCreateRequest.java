package com.ciff.provider.dto;

import com.ciff.common.enums.AuthType;
import com.ciff.common.enums.ProviderType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建供应商请求")
public class ProviderCreateRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 100, message = "名称最长100个字符")
    @Schema(description = "供应商名称", example = "OpenAI")
    private String name;

    @NotNull(message = "类型不能为空")
    @Schema(description = "供应商类型", example = "openai")
    private ProviderType type;

    @NotNull(message = "认证类型不能为空")
    @Schema(description = "认证类型", example = "bearer")
    private AuthType authType;

    @NotBlank(message = "API 地址不能为空")
    @Schema(description = "API 基础地址", example = "https://api.openai.com")
    private String apiBaseUrl;

    @Schema(description = "API Key（明文，不入库）", example = "sk-proj-xxx")
    private String apiKey;

    @Schema(description = "认证扩展配置（如 apiVersion、tokenTtl 等）")
    private ProviderAuthConfig authConfig;
}
