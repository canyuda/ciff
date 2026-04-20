package com.ciff.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseErrorEvent {
    private String message;
}
