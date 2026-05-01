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
@TableName(value = "t_workflow_execution", autoResultMap = true)
public class WorkflowExecutionPO extends BaseEntity {

    private String taskId;
    private Long workflowId;
    private Long userId;
    private String status;
    private String currentStepId;
    private String currentStepName;
    private Integer completedSteps;
    private Integer totalSteps;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> inputs;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> finalOutputs;

    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
