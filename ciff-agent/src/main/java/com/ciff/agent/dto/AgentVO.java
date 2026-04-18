package com.ciff.agent.dto;

import com.ciff.mcp.dto.ToolVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "Agent 响应")
public class AgentVO {

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "Agent 名称")
    private String name;

    @Schema(description = "Agent 描述")
    private String description;

    @Schema(description = "Agent 类型：chatbot / agent / workflow")
    private String type;

    @Schema(description = "绑定模型 ID")
    private Long modelId;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "工作流 ID")
    private Long workflowId;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "模型参数覆盖")
    private Map<String, Object> modelParams;

    @Schema(description = "回退模型 ID")
    private Long fallbackModelId;

    @Schema(description = "回退模型名称")
    private String fallbackModelName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "绑定的工具列表")
    private List<ToolVO> tools;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
