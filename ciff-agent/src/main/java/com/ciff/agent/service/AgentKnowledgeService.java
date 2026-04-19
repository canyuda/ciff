package com.ciff.agent.service;

import com.ciff.knowledge.dto.KnowledgeVO;

import java.util.List;

public interface AgentKnowledgeService {

    /** Bind a knowledge base to an agent. Throws if already bound or knowledge not found. */
    void bind(Long agentId, Long knowledgeId);

    /** Unbind a knowledge base from an agent. */
    void unbind(Long agentId, Long knowledgeId);

    /** Replace all knowledge bindings for an agent (full replacement). */
    void replaceAll(Long agentId, List<Long> knowledgeIds);

    /** Get all knowledge bases bound to an agent. */
    List<KnowledgeVO> listKnowledges(Long agentId);

    /** Get all knowledge IDs bound to an agent. */
    List<Long> listKnowledgeIds(Long agentId);
}
