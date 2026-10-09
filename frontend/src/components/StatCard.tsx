import React, { ReactNode } from 'react';
import './StatCard.scss';

interface StatCardProps {
  title: string;
  value: number | string;
  icon: ReactNode;
  variant?: 'primary' | 'success' | 'warning' | 'danger' | 'purple' | 'neutral';
  subtitle?: string;
  onClick?: () => void;
  active?: boolean;
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  icon,
  variant = 'primary',
  subtitle,
  onClick,
  active = false,
}) => {
  return (
    <div
      className={`stat-card stat-card--${variant} ${active ? 'stat-card--active' : ''} ${onClick ? 'stat-card--clickable' : ''}`}
      onClick={onClick}
    >
      <div className="stat-card__header">
        <span className="stat-card__title">{title}</span>
        <div className="stat-card__icon">{icon}</div>
      </div>
      <div className="stat-card__body">
        <span className="stat-card__value">{value}</span>
        {subtitle && <span className="stat-card__subtitle">{subtitle}</span>}
      </div>
    </div>
  );
};
