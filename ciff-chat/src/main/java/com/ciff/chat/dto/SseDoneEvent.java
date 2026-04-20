package com.ciff.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseDoneEvent {
    private SseTokenUsage tokenUsage;
    private int latencyMs;

    @Data
    @Builder
    public static class SseTokenUsage {
        private int promptTokens;
        private int completionTokens;
    }
}
