-- Flyway Migration V1: Initial Schema for DevTrack Tasks
-- Purpose: Create tasks table with constraints, indexes, and audit timestamps.

CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    due_date DATE,
    owner_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_task_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_task_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'IN_REVIEW', 'COMPLETED'))
);

-- Indexes for performance and multi-tenant security queries
CREATE INDEX idx_tasks_owner_id ON tasks(owner_id);
CREATE INDEX idx_tasks_owner_status ON tasks(owner_id, status);
CREATE INDEX idx_tasks_owner_priority ON tasks(owner_id, priority);
CREATE INDEX idx_tasks_owner_due_date ON tasks(owner_id, due_date);
CREATE INDEX idx_tasks_created_at ON tasks(created_at DESC);

-- System-level stats indexes
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_priority ON tasks(priority);
