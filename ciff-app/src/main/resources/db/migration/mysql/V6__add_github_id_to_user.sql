ALTER TABLE t_user ADD COLUMN github_id BIGINT NULL COMMENT 'GitHub user ID' AFTER role;
ALTER TABLE t_user ADD UNIQUE INDEX uk_github_id (github_id);
