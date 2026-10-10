-- Flyway Migration V2: Add optimistic locking version column to tasks table
-- Purpose: Prevent concurrent update overwrites via JPA @Version optimistic locking.

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
