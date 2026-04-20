package com.ciff.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "对话响应")
public class ChatResponse {

    @Schema(description = "会话 ID")
    private Long conversationId;

    @Schema(description = "是否新建会话")
    private boolean newConversation;

    @Schema(description = "用户消息 ID")
    private Long userMessageId;

    @Schema(description = "助手消息 ID")
    private Long assistantMessageId;

    @Schema(description = "助手回复内容")
    private String content;

    @Schema(description = "Token 用量")
    private TokenUsage tokenUsage;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "延迟(ms)")
    private Integer latencyMs;
}
