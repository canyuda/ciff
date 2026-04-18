package com.ciff.knowledge.facade;

import com.ciff.knowledge.dto.KnowledgeVO;

/**
 * Knowledge facade for cross-module access (ciff-agent / ciff-chat).
 */
public interface KnowledgeFacade {

    KnowledgeVO getById(Long id);
}
