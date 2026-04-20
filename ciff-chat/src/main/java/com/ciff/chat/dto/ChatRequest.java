package com.ciff.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "发送消息请求")
public class ChatRequest {

    @NotNull(message = "agentId 不能为空")
    @Schema(description = "Agent ID", required = true)
    private Long agentId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "用户消息", required = true)
    private String message;

    @Schema(description = "会话 ID，为空则新建会话")
    private Long conversationId;

    @Schema(description = "RAG 模式: RAG_WITHOUT_RERANKER(使用RAG不精排), RAG_WITH_RERANKER(使用RAG带精排), NO_RAG(不使用RAG)。默认 RAG_WITH_RERANKER")
    private RagMode ragMode;

    public RagMode getRagMode() {
        return ragMode != null ? ragMode : RagMode.RAG_WITH_RERANKER;
    }
}
