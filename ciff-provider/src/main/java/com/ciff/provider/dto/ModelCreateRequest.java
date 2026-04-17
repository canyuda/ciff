package com.ciff.provider.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建模型请求")
public class ModelCreateRequest {

    @NotNull(message = "供应商ID不能为空")
    @Schema(description = "所属供应商ID", example = "1")
    private Long providerId;

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称最长100个字符")
    @Schema(description = "模型名称（如 gpt-4o）", example = "gpt-4o")
    private String name;

    @Schema(description = "模型显示名称", example = "GPT-4o")
    private String displayName;

    @Schema(description = "最大 token 数", example = "128000")
    private Integer maxTokens;

    @Schema(description = "默认参数（JSON 格式）", example = "{\"temperature\": 0.7, \"top_p\": 1}")
    private String defaultParams;
}
