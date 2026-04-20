package com.ciff.chat.convertor;

import com.ciff.chat.dto.ConversationVO;
import com.ciff.chat.entity.ConversationPO;

public final class ConversationConvertor {

    private ConversationConvertor() {
    }

    public static ConversationVO toVO(ConversationPO po) {
        ConversationVO vo = new ConversationVO();
        vo.setId(po.getId());
        vo.setAgentId(po.getAgentId());
        vo.setTitle(po.getTitle());
        vo.setStatus(po.getStatus());
        vo.setCreateTime(po.getCreateTime());
        vo.setUpdateTime(po.getUpdateTime());
        return vo;
    }
}
