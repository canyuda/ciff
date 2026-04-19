package com.ciff.knowledge.service;

import java.util.List;

/**
 * Rerank service for refining vector search results.
 * Uses Alibaba Cloud gte-rerank-v2 cross-encoder model.
 */
public interface RerankService {

    /**
     * Rerank documents by relevance to the query.
     *
     * @param query     search query
     * @param documents list of document texts to rerank
     * @param topN      max results to return
     * @return reranked results ordered by relevance (desc)
     */
    List<RerankEntry> rerank(String query, List<String> documents, int topN);

    /**
     * Rerank result entry.
     */
    record RerankEntry(int index, double relevanceScore) {
    }
}