package com.ciff.mcp.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.common.entity.SoftDeletableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_tool", autoResultMap = true)
public class ToolPO extends SoftDeletableEntity {

    private String name;

    private String description;

    /** api / mcp */
    private String type;

    /** URL or MCP server address */
    private String endpoint;

    /** input/output JSON schema */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> paramSchema;

    /** auth configuration */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> authConfig;

    /** enabled / disabled */
    private String status;
}
