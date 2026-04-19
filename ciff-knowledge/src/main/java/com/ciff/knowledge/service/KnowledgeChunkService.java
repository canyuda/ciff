package com.ciff.knowledge.service;

import com.ciff.knowledge.entity.KnowledgeChunkPO;

import java.util.List;

public interface KnowledgeChunkService {

    KnowledgeChunkPO getById(Long id);

    List<KnowledgeChunkPO> listByDocumentId(Long documentId);

    List<KnowledgeChunkPO> listByKnowledgeId(Long knowledgeId);

    void batchInsert(List<KnowledgeChunkPO> chunks);

    void deleteById(Long id);

    void deleteByDocumentId(Long documentId);

    void deleteByKnowledgeId(Long knowledgeId);

    /**
     * Vector similarity search using cosine distance.
     *
     * @param embedding    query vector
     * @param knowledgeId  optional filter by knowledge base
     * @param limit        max results
     * @return chunks ordered by similarity (desc)
     */
    List<KnowledgeChunkPO> search(float[] embedding, Long knowledgeId, int limit);
}
