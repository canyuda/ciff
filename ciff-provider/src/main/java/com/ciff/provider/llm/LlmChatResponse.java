package com.ciff.provider.llm;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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

    /** Tool calls made by the model (null if none). */
    private List<ToolCall> toolCalls;

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

    /**
     * Tool call in response.
     */
    @Data
    @Builder
    public static class ToolCall {
        private String id;
        private String type;
        private FunctionCall function;
    }

    /**
     * Function call details.
     */
    @Data
    @Builder
    public static class FunctionCall {
        private String name;
        private String arguments;
    }
}
