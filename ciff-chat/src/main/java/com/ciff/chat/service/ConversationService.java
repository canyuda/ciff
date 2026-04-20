package com.ciff.chat.service;

import com.ciff.chat.dto.ConversationVO;
import com.ciff.common.dto.PageResult;

public interface ConversationService {

    ConversationVO create(Long agentId, String title, Long userId);

    ConversationVO getById(Long id, Long userId);

    PageResult<ConversationVO> page(Integer page, Integer pageSize, Long agentId, Long userId);

    void delete(Long id, Long userId);

    void updateTitle(Long id, String title);
}
