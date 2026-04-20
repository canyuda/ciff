package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    public static class Choice {
        private Delta delta;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String content;
    }
}
