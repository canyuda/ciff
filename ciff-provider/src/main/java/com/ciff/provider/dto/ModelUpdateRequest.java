package com.ciff.provider.dto;

import com.ciff.common.enums.ProviderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新模型请求")
public class ModelUpdateRequest {

    @Schema(description = "所属供应商ID", example = "1")
    private Long providerId;

    @Size(max = 100, message = "模型名称最长100个字符")
    @Schema(description = "模型名称", example = "gpt-4o")
    private String name;

    @Schema(description = "模型显示名称", example = "GPT-4o")
    private String displayName;

    @Schema(description = "最大 token 数", example = "128000")
    private Integer maxTokens;

    @Schema(description = "默认参数（JSON 格式）", example = "{\"temperature\": 0.7}")
    private String defaultParams;

    @Schema(description = "状态", example = "active")
    private ProviderStatus status;
}
