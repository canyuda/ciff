package com.ciff.knowledge.service;

import com.ciff.common.dto.PageResult;
import com.ciff.knowledge.dto.KnowledgeCreateRequest;
import com.ciff.knowledge.dto.KnowledgeUpdateRequest;
import com.ciff.knowledge.dto.KnowledgeVO;

public interface KnowledgeService {

    KnowledgeVO create(KnowledgeCreateRequest request, Long userId);

    KnowledgeVO update(Long id, KnowledgeUpdateRequest request, Long userId);

    KnowledgeVO getById(Long id, Long userId);

    void delete(Long id, Long userId);

    PageResult<KnowledgeVO> page(Integer page, Integer pageSize, String status, Long userId);
}
