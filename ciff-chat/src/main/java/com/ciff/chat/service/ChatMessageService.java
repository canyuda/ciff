package com.ciff.chat.service;

import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.dto.TokenUsage;
import com.ciff.chat.entity.ChatMessagePO;
import com.ciff.common.dto.PageResult;

import java.util.List;

public interface ChatMessageService {

    ChatMessagePO saveUserMessage(Long conversationId, String content);

    ChatMessagePO saveAssistantMessage(Long conversationId, String content,
                                       TokenUsage tokenUsage, String modelName, int latencyMs);

    ChatMessagePO saveToolMessage(Long conversationId, String content, String toolCallId);

    List<ChatMessagePO> listByConversationId(Long conversationId);

    PageResult<ChatMessageVO> page(Long conversationId, Integer page, Integer pageSize);

    void deleteByConversationId(Long conversationId);
}
