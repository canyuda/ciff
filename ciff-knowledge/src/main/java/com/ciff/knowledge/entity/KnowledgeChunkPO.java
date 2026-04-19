package com.ciff.knowledge.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Chunk entity stored in PGVector (PostgreSQL).
 * Mapped via JdbcTemplate RowMapper — not MyBatis-Plus.
 */
@Data
public class KnowledgeChunkPO {

    private Long id;

    private Long documentId;

    private Long knowledgeId;

    private String content;

    /** 1024-dim embedding vector */
    private float[] embedding;

    private Integer chunkIndex;

    /** Cosine similarity score, populated only by search queries */
    private Double similarity;

    private LocalDateTime createTime;
}
