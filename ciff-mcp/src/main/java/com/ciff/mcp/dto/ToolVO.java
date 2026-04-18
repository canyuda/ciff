package com.ciff.mcp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Schema(description = "工具响应")
public class ToolVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "工具名称")
    private String name;

    @Schema(description = "工具描述")
    private String description;

    @Schema(description = "工具类型：api / mcp")
    private String type;

    @Schema(description = "端点地址")
    private String endpoint;

    @Schema(description = "参数 JSON Schema")
    private Map<String, Object> paramSchema;

    @Schema(description = "认证配置")
    private Map<String, Object> authConfig;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
