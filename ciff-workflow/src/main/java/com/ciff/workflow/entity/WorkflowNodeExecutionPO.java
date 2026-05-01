package com.ciff.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_workflow_node_execution", autoResultMap = true)
public class WorkflowNodeExecutionPO extends BaseEntity {

    private Long executionId;
    private String stepId;
    private String stepName;
    private String stepType;
    private String status;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputs;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> outputs;

    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer durationMs;
    private Integer retryCount;
}
