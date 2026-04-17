package com.ciff.provider.llm;

import lombok.Builder;
import lombok.Data;

/**
 * 统一 LLM 聊天响应。
 * 各厂商响应由 LlmChatClient 实现类解析为统一格式。
 */
@Data
@Builder
public class LlmChatResponse {

    private String content;

    private String finishReason;

    private Usage usage;

    /**
     * Token 用量统计。
     */
    @Data
    @Builder
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
}
