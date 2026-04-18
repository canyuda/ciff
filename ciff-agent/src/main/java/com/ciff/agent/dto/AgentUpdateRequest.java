package com.ciff.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "更新 Agent 请求")
public class AgentUpdateRequest {

    @Size(max = 128, message = "名称最长128个字符")
    @Schema(description = "Agent 名称")
    private String name;

    @Size(max = 512, message = "描述最长512个字符")
    @Schema(description = "Agent 描述")
    private String description;

    @Schema(description = "Agent 类型：chatbot / agent / workflow")
    private String type;

    @Schema(description = "绑定模型 ID")
    private Long modelId;

    @Schema(description = "工作流 ID")
    private Long workflowId;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "模型参数覆盖")
    private Map<String, Object> modelParams;

    @Schema(description = "回退模型 ID")
    private Long fallbackModelId;

    @Schema(description = "状态：active / inactive / draft")
    private String status;

    @Schema(description = "替换绑定的工具 ID 列表（全量替换）")
    private List<Long> toolIds;
}
