package com.ciff.provider.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * OpenAI-compatible chat completion request body.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenAiCompatibleRequest {
    private String model;
    private boolean stream;
    private Double temperature;
    private Integer maxTokens;
    private List<LlmChatRequest.Message> messages;
}
