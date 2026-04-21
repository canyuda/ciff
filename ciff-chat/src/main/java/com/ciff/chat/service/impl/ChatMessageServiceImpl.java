package com.ciff.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ciff.chat.convertor.ChatMessageConvertor;
import com.ciff.chat.dto.ChatMessageMetadata;
import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.dto.TokenUsage;
import com.ciff.chat.entity.ChatMessagePO;
import com.ciff.chat.mapper.ChatMessageMapper;
import com.ciff.chat.service.ChatMessageService;
import com.ciff.common.dto.PageResult;
import com.ciff.common.util.PageHelper;
import com.ciff.knowledge.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageMapper chatMessageMapper;
    private final DocumentService documentService;

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
                                               TokenUsage tokenUsage, String modelName, int latencyMs,
                                               List<Long> docIds) {
        ChatMessagePO po = new ChatMessagePO();
        po.setConversationId(conversationId);
        po.setRole("assistant");
        po.setContent(content);
        po.setTokenUsage(tokenUsage);
        po.setModelName(modelName);
        po.setLatencyMs(latencyMs);
        if (docIds != null && !docIds.isEmpty()) {
            ChatMessageMetadata metadata = new ChatMessageMetadata();
            metadata.setRagDocIds(docIds);
            po.setMetadata(metadata);
        }
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

        // Batch resolve reference document names from metadata
        Set<Long> allDocIds = result.getRecords().stream()
                .map(po -> extractDocIds(po))
                .flatMap(List::stream)
                .collect(Collectors.toSet());
        Map<Long, String> docNameMap = allDocIds.isEmpty()
                ? Collections.emptyMap()
                : documentService.getDocumentNamesByIds(allDocIds.stream().toList());

        var records = result.getRecords().stream()
                .map(po -> ChatMessageConvertor.toVO(po, docNameMap))
                .toList();
        return PageResult.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public void deleteByConversationId(Long conversationId) {
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessagePO>()
                        .eq(ChatMessagePO::getConversationId, conversationId));
    }

    private List<Long> extractDocIds(ChatMessagePO po) {
        ChatMessageMetadata metadata = po.getMetadata();
        if (metadata != null && metadata.getRagDocIds() != null) {
            return metadata.getRagDocIds();
        }
        return List.of();
    }
}
