package com.ciff.chat.dto;

/**
 * RAG retrieval mode for chat requests.
 */
public enum RagMode {

    /** Use RAG with reranking (full pipeline: embed → vector search → rerank → filter). */
    RAG_WITH_RERANKER,

    /** Use RAG without reranking (embed → vector search only). */
    RAG_WITHOUT_RERANKER,

    /** Skip RAG entirely. */
    NO_RAG
}
