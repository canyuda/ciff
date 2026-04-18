-- Allow file_path to be NULL for uploading status records (file not yet saved)
ALTER TABLE t_knowledge_document MODIFY COLUMN file_path VARCHAR(512) NULL COMMENT 'storage path';
