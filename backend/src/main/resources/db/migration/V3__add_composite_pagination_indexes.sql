-- Flyway Migration V3: Composite Indexes for Deterministic Pagination & Filter Performance
-- Purpose: Optimize multi-tenant filtered lookups and prevent filesorts on paginated results.

CREATE INDEX IF NOT EXISTS idx_tasks_owner_created_at_id ON tasks(owner_id, created_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_tasks_owner_status_created ON tasks(owner_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_tasks_owner_priority_created ON tasks(owner_id, priority, created_at DESC);
