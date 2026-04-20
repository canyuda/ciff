package com.ciff.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.ciff.chat.dto.TokenUsage;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "t_chat_message", autoResultMap = true)
public class ChatMessagePO {

    private Long id;

    private Long conversationId;

    /** user / assistant / tool */
    private String role;

    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private TokenUsage tokenUsage;

    private String modelName;

    private Integer latencyMs;

    private String toolCallId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object metadata;

    private LocalDateTime createTime;
}
