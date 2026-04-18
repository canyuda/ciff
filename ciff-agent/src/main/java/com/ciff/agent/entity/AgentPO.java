package com.ciff.agent.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

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

    /** override: temperature, max_tokens, etc. */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> modelParams;

    private Long fallbackModelId;

    /** active / inactive / draft */
    private String status;
}
