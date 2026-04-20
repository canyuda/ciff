package com.ciff.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * OpenAI-compatible chat completion request body.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class OpenAiChatRequest {
    private String model;
    private BigDecimal temperature;
    private Integer maxTokens;
    private boolean stream;
    private List<LlmMessage> messages;
    private List<?> tools;
}
