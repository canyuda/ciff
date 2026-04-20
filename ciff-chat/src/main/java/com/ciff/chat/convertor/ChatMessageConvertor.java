package com.ciff.chat.convertor;

import com.ciff.chat.dto.ChatMessageVO;
import com.ciff.chat.entity.ChatMessagePO;

public final class ChatMessageConvertor {

    private ChatMessageConvertor() {
    }

    public static ChatMessageVO toVO(ChatMessagePO po) {
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(po.getId());
        vo.setConversationId(po.getConversationId());
        vo.setRole(po.getRole());
        vo.setContent(po.getContent());
        vo.setTokenUsage(po.getTokenUsage());
        vo.setModelName(po.getModelName());
        vo.setLatencyMs(po.getLatencyMs());
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }
}
