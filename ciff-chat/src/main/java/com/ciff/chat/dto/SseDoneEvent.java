package com.ciff.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SseDoneEvent {
    private SseTokenUsage tokenUsage;
    private int latencyMs;
    private List<String> referenceDocuments;

    @Data
    @Builder
    public static class SseTokenUsage {
        private int promptTokens;
        private int completionTokens;
    }
}
