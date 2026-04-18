-- Test table for PGVector datasource verification
CREATE TABLE IF NOT EXISTS pg_demo_item (
    id          BIGSERIAL      PRIMARY KEY,
    name        VARCHAR(128)   NOT NULL,
    status      VARCHAR(32)    NOT NULL DEFAULT 'active',
    create_time TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pg_demo_item_status ON pg_demo_item (status);
