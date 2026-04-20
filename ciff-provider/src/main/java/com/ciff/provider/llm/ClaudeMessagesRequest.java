package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Claude Messages API request body.
 * System prompt is extracted to a top-level field (not in messages array).
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ClaudeMessagesRequest {
    private String model;
    private boolean stream;
    private int maxTokens;
    private Double temperature;
    private String system;
    private List<LlmChatRequest.Message> messages;
}
