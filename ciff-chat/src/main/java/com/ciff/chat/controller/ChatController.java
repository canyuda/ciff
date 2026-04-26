package com.ciff.chat.controller;

import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.dto.ConversationVO;
import com.ciff.chat.service.ChatMessageService;
import com.ciff.chat.service.ConversationService;
import com.ciff.common.context.UserContext;
import com.ciff.common.dto.PageResult;
import com.ciff.common.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
@Tag(name = "会话管理", description = "会话 CRUD + 消息分页")
public class ChatController {

    private final ConversationService conversationService;
    private final ChatMessageService chatMessageService;

    @GetMapping
    @Operation(summary = "分页查询会话列表")
    public Result<PageResult<ConversationVO>> page(
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "Agent ID 筛选") @RequestParam(required = false) Long agentId) {
        return Result.ok(conversationService.page(page, pageSize, agentId, UserContext.getUserId()));
    }

    // todo [未被前端使用:待清理]
    @GetMapping("/{id}")
    @Operation(summary = "查询会话详情")
    public Result<ConversationVO> getById(
            @Parameter(description = "会话 ID") @PathVariable Long id) {
        return Result.ok(conversationService.getById(id, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除会话（级联删除消息）")
    public Result<Void> delete(
            @Parameter(description = "会话 ID") @PathVariable Long id) {
        chatMessageService.deleteByConversationId(id);
        conversationService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "分页查询会话消息")
    public Result<PageResult<ChatMessageVO>> pageMessages(
            @Parameter(description = "会话 ID") @PathVariable Long conversationId,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize) {
        return Result.ok(chatMessageService.page(conversationId, page, pageSize));
    }
}
