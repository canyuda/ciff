package com.ciff.knowledge.service;

import java.util.List;

/**
 * Embedding generation service.
 * Calls Alibaba Cloud DashScope text-embedding-v3 API.
 */
public interface EmbeddingService {

    /**
     * Generate embeddings for the given texts.
     * Returns a list of float arrays in the same order as input.
     *
     * @param texts list of text content to embed
     * @return list of embedding vectors, one per input text
     */
    List<float[]> embed(List<String> texts);
}
