package com.ciff.knowledge.facade;

import com.ciff.knowledge.dto.KnowledgeVO;
import com.ciff.knowledge.entity.KnowledgeChunkPO;

import java.util.List;

/**
 * Knowledge facade for cross-module access (ciff-agent / ciff-chat).
 */
public interface KnowledgeFacade {

    KnowledgeVO getById(Long id);

    /**
     * Batch query knowledge bases by IDs (for Agent binding display).
     */
    List<KnowledgeVO> listByIds(List<Long> ids);

    /**
     * Retrieve relevant chunks for RAG with optional reranking.
     *
     * @param query        user query text
     * @param knowledgeIds knowledge base IDs to search within
     * @param topN         max results to return
     * @param useReranker  whether to apply reranking
     * @return ranked chunks with relevance scores
     */
    List<KnowledgeChunkPO> retrieve(String query, List<Long> knowledgeIds, int topN, boolean useReranker);
}
