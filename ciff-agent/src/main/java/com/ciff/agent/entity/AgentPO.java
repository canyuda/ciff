package com.ciff.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.agent.dto.AgentModelParam;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_agent", autoResultMap = true)
public class AgentPO extends SoftDeletableEntity {

    private Long userId;

    private Long modelId;

    private Long workflowId;

    private String name;

    private String description;

    /** chatbot / agent / workflow */
    private String type;

    private String systemPrompt;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private AgentModelParam modelParams;

    private Long fallbackModelId;

    /** active / inactive / draft */
    private String status;
}
