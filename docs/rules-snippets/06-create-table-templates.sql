-- MySQL 建表模板
CREATE TABLE t_{module}_{entity} (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    -- ... 业务字段 ...
    -- 注意：所有字段 NOT NULL，空值用 '' 或 0 代替
    -- 注意：枚举用 VARCHAR(32)，不用 ENUM
    -- 注意：金额/Token 用 BIGINT 存最小精度

    deleted         TINYINT    NOT NULL DEFAULT 0   COMMENT '0=normal, 1=deleted',
    create_time      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time      DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    -- 索引
    INDEX idx_{table}_{column} ({column}),
    UNIQUE INDEX uk_{table}_{column} ({column})
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='{table description}';

-- PGVector 建表模板
CREATE TABLE t_knowledge_chunk (
    id              BIGSERIAL PRIMARY KEY,
    -- ... 业务字段 ...
    embedding       VECTOR(1024) NOT NULL,
    create_time      TIMESTAMP DEFAULT NOW()
);

-- 建表后立即创建 HNSW 索引
CREATE INDEX idx_chunk_embedding ON t_knowledge_chunk
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
