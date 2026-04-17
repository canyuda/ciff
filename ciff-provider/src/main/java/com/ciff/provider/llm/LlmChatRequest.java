package com.ciff.provider.llm;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 统一 LLM 聊天请求。
 * 屏蔽各厂商请求格式差异，由 LlmChatClient 实现类负责转换为厂商特定格式。
 */
@Data
@Builder
public class LlmChatRequest {

    private String modelName;

    private List<Message> messages;

    private Double temperature;

    private Integer maxTokens;

    private Boolean stream;

    /**
     * 聊天消息。
     */
    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
