package com.ciff.chat.convertor;

import com.ciff.chat.dto.ChatMessageMetadata;
import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.entity.ChatMessagePO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ChatMessageConvertor {

    private ChatMessageConvertor() {
    }

    public static ChatMessageVO toVO(ChatMessagePO po) {
        return toVO(po, Collections.emptyMap());
    }

    public static ChatMessageVO toVO(ChatMessagePO po, Map<Long, String> docNameMap) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(po.getId());
        vo.setConversationId(po.getConversationId());
        vo.setRole(po.getRole());
        vo.setContent(po.getContent());
        vo.setTokenUsage(po.getTokenUsage());
        vo.setModelName(po.getModelName());
        vo.setLatencyMs(po.getLatencyMs());
        vo.setReferenceDocuments(extractReferenceDocs(po.getMetadata(), docNameMap));
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    private static List<String> extractReferenceDocs(ChatMessageMetadata metadata,
                                                       Map<Long, String> docNameMap) {
        if (metadata == null || metadata.getRagDocIds() == null || metadata.getRagDocIds().isEmpty()) {
            return Collections.emptyList();
        }
        return metadata.getRagDocIds().stream()
                .map(docNameMap::get)
                .filter(name -> name != null && !name.isEmpty())
                .toList();
    }
}
