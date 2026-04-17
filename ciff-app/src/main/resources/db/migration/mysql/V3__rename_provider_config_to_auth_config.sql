-- V3: Rename t_provider.config to auth_config
ALTER TABLE t_provider CHANGE COLUMN config auth_config JSON NULL COMMENT 'auth extra params: {"api_version":"2023-06-01"}, {"token_ttl":3600}';
