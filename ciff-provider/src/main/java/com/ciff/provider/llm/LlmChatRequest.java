package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

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

    private List<ToolDefinition> tools;

    /**
     * 聊天消息。
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Message {
        private String role;
        private String content;

        /** Tool calls made by assistant (OpenAI format). */
        private List<ToolCall> toolCalls;

        /** Tool call ID for tool result messages. */
        private String toolCallId;
    }

    /**
     * Tool call definition in message.
     */
    @Data
    @Builder
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class FunctionCall {
        private String name;
        private String arguments;
    }

    /**
     * Tool definition for request.
     */
    @Data
    @Builder
    public static class ToolDefinition {
        private String type;
        private FunctionDefinition function;
    }

    /**
     * Function definition.
     */
    @Data
    @Builder
    public static class FunctionDefinition {
        private String name;
        private String description;
        private Map<String, Object> parameters;
    }
}
