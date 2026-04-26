package com.ciff.app.controller;

import com.ciff.app.entity.ApiKeyPO;
import com.ciff.app.service.ApiKeyService;
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
@RequestMapping("/api/v1/external/chat")
@RequiredArgsConstructor
@Tag(name = "外部对话接口", description = "通过 API Key 调用的外部对话接口")
public class ExternalChatController {

    private final ChatService chatService;

    // todo [未被前端使用:设计性保留]
    @PostMapping
    @Operation(summary = "外部对话（非流式）")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(request, UserContext.getUserId());
        return Result.ok(response);
    }

    // todo [未被前端使用:设计性保留]
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "外部对话（SSE 流式）")
    public SseEmitter streamChat(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(request, UserContext.getUserId());
    }
}
