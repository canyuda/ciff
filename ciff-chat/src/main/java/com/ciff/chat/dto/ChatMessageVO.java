package com.ciff.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "聊天消息响应")
public class ChatMessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "会话 ID")
    private Long conversationId;

    @Schema(description = "角色：user / assistant / tool")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "Token 用量")
    private TokenUsage tokenUsage;

    @Schema(description = "模型名称")
    private String modelName;

    @Schema(description = "延迟(ms)")
    private Integer latencyMs;

    @Schema(description = "参考文档列表")
    private java.util.List<String> referenceDocuments;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
