package com.ciff.chat.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single tool call entry in OpenAI-compatible request format.
 */
@Data
@Builder
public class ToolCallEntry {
    private String id;
    private String type;
    private FunctionDef function;

    @Data
    @Builder
    public static class FunctionDef {
        private String name;
        private String arguments;
    }
}
