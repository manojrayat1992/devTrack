import React from 'react';
import { TaskStatus } from '../types';
import './Badges.scss';

interface StatusBadgeProps {
  status: TaskStatus;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  const formatLabel = (s: TaskStatus) => {
    switch (s) {
      case 'OPEN':
        return 'Open';
      case 'IN_PROGRESS':
        return 'In Progress';
      case 'IN_REVIEW':
        return 'In Review';
      case 'COMPLETED':
        return 'Completed';
      default:
        return s;
    }
  };

  return (
    <span className={`badge badge--status badge--status-${status.toLowerCase()}`}>
      <span className="badge__dot" />
      {formatLabel(status)}
    </span>
  );
};
