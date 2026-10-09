import React from 'react';
import { TaskFilterCriteria, TaskPriority, TaskStatus } from '../types';
import { Search, RotateCcw, Filter } from 'lucide-react';
import './TaskFilterBar.scss';

interface TaskFilterBarProps {
  criteria: TaskFilterCriteria;
  onChange: (criteria: TaskFilterCriteria) => void;
  onReset: () => void;
}

export const TaskFilterBar: React.FC<TaskFilterBarProps> = ({
  criteria,
  onChange,
  onReset,
}) => {
  const hasActiveFilters = Boolean(
    criteria.search ||
    criteria.status ||
    criteria.priority ||
    criteria.dueBefore ||
    criteria.dueAfter
  );

  return (
    <div className="filter-bar">
      <div className="filter-bar__search">
        <Search size={16} className="filter-bar__search-icon" />
        <input
          type="text"
          value={criteria.search || ''}
          onChange={(e) => onChange({ ...criteria, search: e.target.value })}
          placeholder="Search by title or description..."
          className="filter-bar__search-input"
        />
      </div>

      <div className="filter-bar__controls">
        <div className="filter-bar__control">
          <label htmlFor="filter-status" className="filter-bar__label">
            <Filter size={13} /> Status:
          </label>
          <select
            id="filter-status"
            value={criteria.status || ''}
            onChange={(e) =>
              onChange({ ...criteria, status: e.target.value as TaskStatus | '' })
            }
          >
            <option value="">All Statuses</option>
            <option value="OPEN">Open</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="IN_REVIEW">In Review</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>

        <div className="filter-bar__control">
          <label htmlFor="filter-priority" className="filter-bar__label">
            Priority:
          </label>
          <select
            id="filter-priority"
            value={criteria.priority || ''}
            onChange={(e) =>
              onChange({ ...criteria, priority: e.target.value as TaskPriority | '' })
            }
          >
            <option value="">All Priorities</option>
            <option value="LOW">Low</option>
            <option value="MEDIUM">Medium</option>
            <option value="HIGH">High</option>
            <option value="CRITICAL">Critical</option>
          </select>
        </div>

        {hasActiveFilters && (
          <button
            type="button"
            onClick={onReset}
            className="filter-bar__reset-btn"
            title="Reset filters"
          >
            <RotateCcw size={14} />
            <span>Clear</span>
          </button>
        )}
      </div>
    </div>
  );
};
