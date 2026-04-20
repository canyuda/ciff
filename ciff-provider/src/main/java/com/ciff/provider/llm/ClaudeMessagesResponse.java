package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;

/**
 * Claude Messages API response.
 * Format: {"content":[{type,text,...}],"usage":{...},"stop_reason":"end_turn"}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClaudeMessagesResponse {
    private List<ContentBlock> content;
    private Usage usage;
    private String stopReason;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ContentBlock {
        private String type;
        private String text;
        private String id;
        private String name;
        private JsonNode input;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Usage {
        private Integer inputTokens;
        private Integer outputTokens;
    }
}
