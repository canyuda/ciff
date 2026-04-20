package com.ciff.chat.service;

import com.ciff.chat.dto.ChatRequest;
import com.ciff.chat.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    /**
     * Non-streaming chat: send message → get full response.
     */
    ChatResponse chat(ChatRequest request, Long userId);

    /**
     * SSE streaming chat: send message → receive tokens via SseEmitter.
     */
    SseEmitter streamChat(ChatRequest request, Long userId);
}
