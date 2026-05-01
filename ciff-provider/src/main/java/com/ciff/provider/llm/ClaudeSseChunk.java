package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/**
 * Claude SSE streaming chunk.
 * Format: {"type":"content_block_delta","delta":{"type":"text_delta","text":"xxx"}}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClaudeSseChunk {
    private String type;
    private Delta delta;
    private ContentBlock contentBlock;
    private String stopReason;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String type;
        private String text;
        /** For tool_use: incremental JSON fragment. */
        private String partialJson;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentBlock {
        private String type;
        private String id;
        private String name;
    }
}
