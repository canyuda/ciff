package com.ciff.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.chat.convertor.ChatMessageConvertor;
import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.dto.TokenUsage;
import com.ciff.chat.entity.ChatMessagePO;
import com.ciff.chat.mapper.ChatMessageMapper;
import com.ciff.chat.service.ChatMessageService;
import com.ciff.common.dto.PageResult;
import com.ciff.common.util.PageHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;

    @Override
    public ChatMessagePO saveUserMessage(Long conversationId, String content) {
        ChatMessagePO po = new ChatMessagePO();
        po.setConversationId(conversationId);
        po.setRole("user");
        po.setContent(content);
        chatMessageMapper.insert(po);
        return po;
    }

    @Override
    public ChatMessagePO saveAssistantMessage(Long conversationId, String content,
                                               TokenUsage tokenUsage, String modelName, int latencyMs) {
        ChatMessagePO po = new ChatMessagePO();
        po.setConversationId(conversationId);
        po.setRole("assistant");
        po.setContent(content);
        po.setTokenUsage(tokenUsage);
        po.setModelName(modelName);
        po.setLatencyMs(latencyMs);
        chatMessageMapper.insert(po);
        return po;
    }

    @Override
    public ChatMessagePO saveToolMessage(Long conversationId, String content, String toolCallId) {
        ChatMessagePO po = new ChatMessagePO();
        po.setConversationId(conversationId);
        po.setRole("tool");
        po.setContent(content);
        po.setToolCallId(toolCallId);
        chatMessageMapper.insert(po);
        return po;
    }

    @Override
    public List<ChatMessagePO> listByConversationId(Long conversationId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessagePO>()
                        .eq(ChatMessagePO::getConversationId, conversationId)
                        .orderByAsc(ChatMessagePO::getCreateTime));
    }

    @Override
    public PageResult<ChatMessageVO> page(Long conversationId, Integer page, Integer pageSize) {
        Page<ChatMessagePO> pageParam = PageHelper.toPage(page, pageSize);

        LambdaQueryWrapper<ChatMessagePO> wrapper = new LambdaQueryWrapper<ChatMessagePO>()
                .eq(ChatMessagePO::getConversationId, conversationId)
                .orderByAsc(ChatMessagePO::getCreateTime);

        Page<ChatMessagePO> result = chatMessageMapper.selectPage(pageParam, wrapper);
        var records = result.getRecords().stream().map(ChatMessageConvertor::toVO).toList();
        return PageResult.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessagePO>()
                        .eq(ChatMessagePO::getConversationId, conversationId));
    }
}
