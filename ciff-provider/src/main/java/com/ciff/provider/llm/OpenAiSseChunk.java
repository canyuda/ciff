package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible SSE streaming chunk.
 * Format: {"choices":[{"delta":{"content":"xxx"}}]}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiSseChunk {
    private List<Choice> choices;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Choice {
        private Delta delta;
        private String finishReason;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Delta {
        private String content;
        private List<ToolCallDelta> toolCalls;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ToolCallDelta {
        private Integer index;
        private String id;
        private String type;
        private FunctionDelta function;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FunctionDelta {
        private String name;
        private String arguments;
    }
}
