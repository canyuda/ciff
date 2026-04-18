package com.ciff.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
@Schema(description = "更新工具请求")
public class ToolUpdateRequest {

    @Size(max = 128, message = "名称最长128个字符")
    @Schema(description = "工具名称")
    private String name;

    @Size(max = 512, message = "描述最长512个字符")
    @Schema(description = "工具描述")
    private String description;

    @Schema(description = "工具类型：api / mcp")
    private String type;

    @Size(max = 512, message = "端点地址最长512个字符")
    @Schema(description = "URL 或 MCP server 地址")
    private String endpoint;

    @Schema(description = "参数 JSON Schema")
    private Map<String, Object> paramSchema;

    @Schema(description = "认证配置")
    private Map<String, Object> authConfig;

    @Schema(description = "状态：enabled / disabled")
    private String status;
}
