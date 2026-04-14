-- ============================================================
-- Ciff PGVector Schema
-- PostgreSQL + PGVector extension
-- Separate from MySQL schema - run against PGVector instance
-- ============================================================

-- Enable vector extension
CREATE EXTENSION IF NOT EXISTS vector;

-- -----------------------------------------------------------
-- t_knowledge_chunk
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_knowledge_chunk (
    id              BIGSERIAL              PRIMARY KEY,
    document_id     BIGINT                 NOT NULL,
    knowledge_id    BIGINT                 NOT NULL,
    content         TEXT                   NOT NULL,
    embedding       VECTOR(1536)           NOT NULL,
    chunk_index     INT                    NOT NULL,
    create_time     TIMESTAMP              NOT NULL DEFAULT NOW()
);

-- HNSW index for cosine similarity search
CREATE INDEX IF NOT EXISTS idx_chunk_embedding ON t_knowledge_chunk
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- Index for filtering by knowledge_id before vector search
CREATE INDEX IF NOT EXISTS idx_chunk_knowledge_id ON t_knowledge_chunk (knowledge_id);

-- Index for filtering by document_id
CREATE INDEX IF NOT EXISTS idx_chunk_document_id ON t_knowledge_chunk (document_id);
