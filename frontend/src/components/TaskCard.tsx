import React from 'react';
import { Task, TaskStatus } from '../types';
import { StatusBadge } from './StatusBadge';
import { PriorityBadge } from './PriorityBadge';
import { Calendar, Edit3, Trash2, ArrowRightCircle, AlertTriangle } from 'lucide-react';
import './TaskCard.scss';

interface TaskCardProps {
  task: Task;
  onEdit: (task: Task) => void;
  onDelete: (taskId: string) => void;
  onStatusTransition: (taskId: string, newStatus: TaskStatus) => void;
  isTransitioning?: boolean;
}

export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  onEdit,
  onDelete,
  onStatusTransition,
  isTransitioning = false,
}) => {
  const isOverdue =
    task.dueDate &&
    task.status !== 'COMPLETED' &&
    new Date(task.dueDate).setHours(0, 0, 0, 0) < new Date().setHours(0, 0, 0, 0);

  const formatTransitionLabel = (status: TaskStatus) => {
    switch (status) {
      case 'OPEN':
        return 'Reopen';
      case 'IN_PROGRESS':
        return 'Start Work';
      case 'IN_REVIEW':
        return 'Submit for Review';
      case 'COMPLETED':
        return 'Complete';
      default:
        return status;
    }
  };

  return (
    <div className={`task-card ${isOverdue ? 'task-card--overdue' : ''}`}>
      <div className="task-card__header">
        <div className="task-card__badges">
          <PriorityBadge priority={task.priority} />
          <StatusBadge status={task.status} />
        </div>
        <div className="task-card__actions">
          <button
            type="button"
            className="task-card__action-btn"
            onClick={() => onEdit(task)}
            title="Edit task"
            aria-label="Edit task"
          >
            <Edit3 size={15} />
          </button>
          <button
            type="button"
            className="task-card__action-btn task-card__action-btn--delete"
            onClick={() => onDelete(task.id)}
            title="Delete task"
            aria-label="Delete task"
          >
            <Trash2 size={15} />
          </button>
        </div>
      </div>

      <h3 className="task-card__title">{task.title}</h3>

      {task.description && (
        <p className="task-card__description">{task.description}</p>
      )}

      <div className="task-card__meta">
        {task.dueDate && (
          <div className={`task-card__due ${isOverdue ? 'task-card__due--alert' : ''}`}>
            {isOverdue ? <AlertTriangle size={14} /> : <Calendar size={14} />}
            <span>Due {task.dueDate}</span>
          </div>
        )}
      </div>

      {task.allowedNextStatuses && task.allowedNextStatuses.length > 0 && (
        <div className="task-card__transitions">
          <span className="task-card__transition-label">Next step:</span>
          <div className="task-card__transition-buttons">
            {task.allowedNextStatuses.map((nextStatus) => (
              <button
                key={nextStatus}
                type="button"
                disabled={isTransitioning}
                onClick={() => onStatusTransition(task.id, nextStatus)}
                className={`task-card__transition-btn task-card__transition-btn--${nextStatus.toLowerCase()}`}
              >
                <ArrowRightCircle size={13} />
                <span>{formatTransitionLabel(nextStatus)}</span>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
