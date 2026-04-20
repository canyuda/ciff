package com.ciff.app.controller;

import com.ciff.chat.dto.ChatRequest;
import com.ciff.chat.dto.ChatResponse;
import com.ciff.chat.service.ChatService;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/app/chat")
@RequiredArgsConstructor
@Tag(name = "对话接口", description = "发送消息（非流式/流式）")
public class AppChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(summary = "发送消息（非流式）")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request, UserContext.getUserId());
        return Result.ok(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送消息（SSE 流式）")
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(request, UserContext.getUserId());
    }
}
