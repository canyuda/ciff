package com.ciff.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.common.entity.SoftDeletableEntity;
import com.ciff.workflow.dto.WorkflowDefinition;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_workflow", autoResultMap = true)
public class WorkflowPO extends SoftDeletableEntity {
    private Long userId;
    private String name;
    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private WorkflowDefinition definition;

    private String status; // active / inactive / draft
}
