-- ============================================================
-- Ciff Database Schema
-- MySQL 8.x / Charset: utf8mb4 / Engine: InnoDB
-- ============================================================

-- -----------------------------------------------------------
-- t_user
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_user (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    username        VARCHAR(64)            NOT NULL COMMENT 'login name',
    password        VARCHAR(128)           NOT NULL COMMENT 'bcrypt hash',
    role            VARCHAR(32)            NOT NULL DEFAULT 'user' COMMENT 'admin / user',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    UNIQUE INDEX uk_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user account';

-- -----------------------------------------------------------
-- t_provider
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_provider (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    name            VARCHAR(64)            NOT NULL COMMENT 'display name, e.g. OpenAI / Claude / Ollama',
    type            VARCHAR(32)            NOT NULL COMMENT 'openai / claude / gemini / ollama',
    auth_type       VARCHAR(32)            NOT NULL DEFAULT 'bearer' COMMENT 'bearer / api_key_header / jwt / dual_key',
    api_base_url    VARCHAR(512)           NOT NULL COMMENT 'API base URL',
    api_key_encrypted VARCHAR(1024)        NOT NULL DEFAULT '' COMMENT 'AES encrypted API key',
    status          VARCHAR(32)            NOT NULL DEFAULT 'enabled' COMMENT 'enabled / disabled',
    config          JSON                   NULL COMMENT 'auth extra params: {"api_version":"2023-06-01"}, {"token_ttl":3600}',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_provider_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='model provider';

-- -----------------------------------------------------------
-- t_model
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_model (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    provider_id     BIGINT                 NOT NULL COMMENT 'FK t_provider.id',
    name            VARCHAR(128)           NOT NULL COMMENT 'model id, e.g. gpt-4o / claude-sonnet-4-5',
    display_name    VARCHAR(128)           NOT NULL DEFAULT '' COMMENT 'display name',
    max_tokens      INT                    NOT NULL DEFAULT 4096 COMMENT 'max output tokens',
    default_params  JSON                   NULL COMMENT 'default params: temperature, top_p, etc.',
    status          VARCHAR(32)            NOT NULL DEFAULT 'enabled' COMMENT 'enabled / disabled',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_model_provider_id (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='model configuration';

-- -----------------------------------------------------------
-- t_tool
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_tool (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    name            VARCHAR(128)           NOT NULL COMMENT 'tool name',
    description     VARCHAR(512)           NOT NULL DEFAULT '' COMMENT 'tool description',
    type            VARCHAR(32)            NOT NULL COMMENT 'api / mcp',
    endpoint        VARCHAR(512)           NOT NULL COMMENT 'URL or MCP server address',
    param_schema    JSON                   NULL COMMENT 'input/output JSON schema',
    auth_config     JSON                   NULL COMMENT 'auth configuration',
    status          VARCHAR(32)            NOT NULL DEFAULT 'enabled' COMMENT 'enabled / disabled',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_tool_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='external tool';

-- -----------------------------------------------------------
-- t_knowledge
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_knowledge (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    user_id         BIGINT                 NOT NULL COMMENT 'FK t_user.id',
    name            VARCHAR(128)           NOT NULL COMMENT 'knowledge base name',
    description     VARCHAR(512)           NOT NULL DEFAULT '' COMMENT 'description',
    chunk_size      INT                    NOT NULL DEFAULT 500 COMMENT 'fixed chunk length',
    embedding_model VARCHAR(128)           NOT NULL DEFAULT '' COMMENT 'embedding model name',
    status          VARCHAR(32)            NOT NULL DEFAULT 'active' COMMENT 'active / inactive',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_knowledge_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='knowledge base';

-- -----------------------------------------------------------
-- t_knowledge_document
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_knowledge_document (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    knowledge_id    BIGINT                 NOT NULL COMMENT 'FK t_knowledge.id',
    file_name       VARCHAR(256)           NOT NULL COMMENT 'original file name',
    file_path       VARCHAR(512)           NOT NULL COMMENT 'storage path',
    file_size       BIGINT                 NOT NULL DEFAULT 0 COMMENT 'file size in bytes',
    chunk_count     INT                    NOT NULL DEFAULT 0 COMMENT 'number of chunks',
    status          VARCHAR(32)            NOT NULL DEFAULT 'uploading' COMMENT 'uploading / processing / ready / failed',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_kd_knowledge_id (knowledge_id),
    INDEX idx_kd_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='knowledge document';

-- -----------------------------------------------------------
-- t_agent
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_agent (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    user_id         BIGINT                 NOT NULL COMMENT 'FK t_user.id',
    model_id        BIGINT                 NOT NULL COMMENT 'FK t_model.id',
    workflow_id     BIGINT                 NULL COMMENT 'FK t_workflow.id, nullable',
    name            VARCHAR(128)           NOT NULL COMMENT 'agent name',
    description     VARCHAR(512)           NOT NULL DEFAULT '' COMMENT 'agent description',
    type            VARCHAR(32)            NOT NULL COMMENT 'chatbot / agent / workflow',
    system_prompt   TEXT                   NOT NULL COMMENT 'system prompt',
    model_params    JSON                   NULL COMMENT 'override: temperature, max_tokens, etc.',
    fallback_model_id BIGINT               NULL COMMENT 'FK t_model.id, nullable',
    status          VARCHAR(32)            NOT NULL DEFAULT 'active' COMMENT 'active / inactive / draft',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_agent_user_id (user_id),
    INDEX idx_agent_model_id (model_id),
    INDEX idx_agent_workflow_id (workflow_id),
    INDEX idx_agent_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI agent';

-- -----------------------------------------------------------
-- t_agent_tool (many-to-many)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_agent_tool (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    agent_id        BIGINT                 NOT NULL COMMENT 'FK t_agent.id',
    tool_id         BIGINT                 NOT NULL COMMENT 'FK t_tool.id',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',

    UNIQUE INDEX uk_at_agent_tool (agent_id, tool_id),
    INDEX idx_at_agent_id (agent_id),
    INDEX idx_at_tool_id (tool_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent-tool binding';

-- -----------------------------------------------------------
-- t_agent_knowledge (many-to-many)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_agent_knowledge (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    agent_id        BIGINT                 NOT NULL COMMENT 'FK t_agent.id',
    knowledge_id    BIGINT                 NOT NULL COMMENT 'FK t_knowledge.id',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',

    UNIQUE INDEX uk_ak_agent_knowledge (agent_id, knowledge_id),
    INDEX idx_ak_agent_id (agent_id),
    INDEX idx_ak_knowledge_id (knowledge_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='agent-knowledge binding';

-- -----------------------------------------------------------
-- t_workflow
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_workflow (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    user_id         BIGINT                 NOT NULL COMMENT 'FK t_user.id',
    name            VARCHAR(128)           NOT NULL COMMENT 'workflow name',
    description     VARCHAR(512)           NOT NULL DEFAULT '' COMMENT 'description',
    definition      JSON                   NOT NULL COMMENT 'steps + conditions',
    status          VARCHAR(32)            NOT NULL DEFAULT 'active' COMMENT 'active / inactive / draft',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_workflow_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='workflow';

-- -----------------------------------------------------------
-- t_conversation
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_conversation (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    user_id         BIGINT                 NOT NULL COMMENT 'FK t_user.id',
    agent_id        BIGINT                 NOT NULL COMMENT 'FK t_agent.id',
    title           VARCHAR(256)           NOT NULL DEFAULT '' COMMENT 'conversation title',
    status          VARCHAR(32)            NOT NULL DEFAULT 'active' COMMENT 'active / archived',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    INDEX idx_conv_user_updated (user_id, update_time DESC),
    INDEX idx_conv_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='conversation';

-- -----------------------------------------------------------
-- t_chat_message
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_chat_message (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    conversation_id BIGINT                 NOT NULL COMMENT 'FK t_conversation.id',
    role            VARCHAR(32)            NOT NULL COMMENT 'user / assistant / tool',
    content         TEXT                   NOT NULL COMMENT 'message content',
    token_usage     JSON                   NULL COMMENT '{"prompt_tokens":0,"completion_tokens":0}',
    model_name      VARCHAR(128)           NOT NULL DEFAULT '' COMMENT 'LLM model used',
    latency_ms      INT                    NOT NULL DEFAULT 0 COMMENT 'response latency in ms',
    tool_call_id    VARCHAR(64)            NULL COMMENT 'tool call identifier, nullable',
    metadata        JSON                   NULL COMMENT 'extra metadata',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',

    INDEX idx_msg_conv_created (conversation_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='chat message';

-- -----------------------------------------------------------
-- t_api_key
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_api_key (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    user_id         BIGINT                 NOT NULL COMMENT 'FK t_user.id',
    agent_id        BIGINT                 NOT NULL COMMENT 'FK t_agent.id',
    name            VARCHAR(128)           NOT NULL COMMENT 'key display name',
    key_hash        VARCHAR(128)           NOT NULL COMMENT 'SHA256 hash of raw key',
    key_prefix      VARCHAR(16)            NOT NULL COMMENT 'first 8 chars for identification',
    permissions     JSON                   NULL COMMENT 'allowed operations',
    expires_at      DATETIME               NULL COMMENT 'expiration time, nullable',
    status          VARCHAR(32)            NOT NULL DEFAULT 'active' COMMENT 'active / revoked',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    UNIQUE INDEX uk_api_key_prefix (key_prefix),
    INDEX idx_api_key_user_id (user_id),
    INDEX idx_api_key_agent_id (agent_id),
    INDEX idx_api_key_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API key';

-- -----------------------------------------------------------
-- t_demo_item (CRUD demo)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_demo_item (
    id              BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    name            VARCHAR(100)           NOT NULL COMMENT 'item name',
    status          INT                    NOT NULL DEFAULT 0 COMMENT 'status',
    deleted         TINYINT                NOT NULL DEFAULT 0 COMMENT '0=normal, 1=deleted',
    create_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time     DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='demo item';