import React from 'react';
import { TaskPriority } from '../types';
import './Badges.scss';

interface PriorityBadgeProps {
  priority: TaskPriority;
}

export const PriorityBadge: React.FC<PriorityBadgeProps> = ({ priority }) => {
  return (
    <span className={`badge badge--priority badge--priority-${priority.toLowerCase()}`}>
      {priority}
    </span>
  );
};
