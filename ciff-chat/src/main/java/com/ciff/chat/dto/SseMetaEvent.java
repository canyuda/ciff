package com.ciff.chat.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SseMetaEvent {
    private Long conversationId;
    private boolean newConversation;
}
