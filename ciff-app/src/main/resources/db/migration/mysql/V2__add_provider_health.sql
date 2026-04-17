-- ============================================================
-- V2: Add provider health monitoring table
-- ============================================================

CREATE TABLE IF NOT EXISTS t_provider_health (
    id                   BIGINT AUTO_INCREMENT  PRIMARY KEY COMMENT 'PK',
    provider_id          BIGINT                 NOT NULL COMMENT 'FK t_provider.id',
    status               VARCHAR(32)            NOT NULL DEFAULT 'unknown' COMMENT 'healthy / unhealthy / unknown',
    consecutive_failures INT                    NOT NULL DEFAULT 0 COMMENT 'consecutive failure count',
    last_latency_ms      INT                    NOT NULL DEFAULT 0 COMMENT 'latest probe latency in ms',
    last_success_time    DATETIME               NULL COMMENT 'last successful call time',
    last_failure_time    DATETIME               NULL COMMENT 'last failure time',
    last_failure_reason  VARCHAR(512)           NOT NULL DEFAULT '' COMMENT 'last failure error message',
    last_probe_time      DATETIME               NULL COMMENT 'last probe/check time',
    create_time          DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    update_time          DATETIME               NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',

    UNIQUE INDEX uk_ph_provider_id (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='provider health status';
