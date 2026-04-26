package com.ciff.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建 Agent 请求")
public class AgentCreateRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 128, message = "名称最长128个字符")
    @Schema(description = "Agent 名称", example = "客服助手")
    private String name;

    @Size(max = 512, message = "描述最长512个字符")
    @Schema(description = "Agent 描述")
    private String description;

    @NotBlank(message = "类型不能为空")
    @Schema(description = "Agent 类型：agent / workflow", example = "agent")
    private String type;

    @NotNull(message = "模型ID不能为空")
    @Schema(description = "绑定模型 ID")
    private Long modelId;

    @Schema(description = "工作流 ID（workflow 类型必填）")
    private Long workflowId;

    @Schema(description = "系统提示词")
    private String systemPrompt;

    @Schema(description = "模型参数配置")
    private AgentModelParam modelParams;

    @Schema(description = "绑定的工具 ID 列表")
    private List<Long> toolIds;

    @Schema(description = "绑定的知识库 ID 列表")
    private List<Long> knowledgeIds;
}
