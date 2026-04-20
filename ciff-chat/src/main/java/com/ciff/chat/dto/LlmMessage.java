package com.ciff.chat.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Sealed interface for LLM chat messages.
 * Replaces Map-based message assembly with type-safe records.
 * Records are directly serializable by Jackson for OpenAI-compatible wire format.
 */
public sealed interface LlmMessage {

    String role();

    /** Text message (system / user / assistant content). */
    record Text(String role, String content) implements LlmMessage {
    }

    /** Assistant message carrying tool call requests. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ToolCall(String role, List<ToolCallEntry> toolCalls) implements LlmMessage {
    }

    /** Tool execution result message. */
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record ToolResult(String role, String toolCallId, String content) implements LlmMessage {
    }
}
