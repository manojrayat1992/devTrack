import React, { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { dashboardApi } from '../api/dashboardApi';
import { SystemMetricsResponse } from '../types';
import { StatCard } from '../components/StatCard';
import {
  ShieldAlert,
  Users,
  Layers,
  CheckCircle,
  AlertOctagon,
  Lock,
  RefreshCw,
} from 'lucide-react';
import './AdminDashboardPage.scss';

export const AdminDashboardPage: React.FC = () => {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState<SystemMetricsResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMetrics = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await dashboardApi.getSystemMetrics();
      setMetrics(data);
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      setError(errObj.message || 'Failed to fetch admin metrics.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.isAdmin) {
      fetchMetrics();
    }
  }, [user]);

  if (!user?.isAdmin) {
    return (
      <div className="admin-page">
        <div className="admin-unauthorized">
          <ShieldAlert size={48} className="shield-icon" />
          <h2>Administrator Access Required</h2>
          <p>
            You do not have the <code>ROLE_ADMIN</code> privilege required to inspect system telemetry.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="admin-page">
      <div className="admin-header">
        <div>
          <div className="admin-badge">
            <ShieldAlert size={14} /> System Telemetry
          </div>
          <h1 className="admin-title">Platform Health & Metrics</h1>
          <p className="admin-subtitle">
            System-level task metrics aggregated across all developers.
          </p>
        </div>
        <button type="button" onClick={fetchMetrics} className="btn-refresh" disabled={loading}>
          <RefreshCw size={16} className={loading ? 'spin' : ''} />
          <span>Refresh Metrics</span>
        </button>
      </div>

      <div className="admin-privacy-banner">
        <Lock size={18} className="lock-icon" />
        <div className="banner-text">
          <strong>Privacy by Architecture:</strong> Administrators can view high-level system volume, status ratios, and health indicators, but <em>cannot</em> view or mutate private task contents owned by developers.
        </div>
      </div>

      {error && (
        <div className="admin-error">
          <AlertOctagon size={18} />
          <span>{error}</span>
        </div>
      )}

      {loading && !metrics ? (
        <div className="admin-loading">
          <RefreshCw size={32} className="spin" />
          <span>Calculating telemetry...</span>
        </div>
      ) : metrics ? (
        <div className="admin-content">
          <div className="admin-stats-grid">
            <StatCard
              title="Total System Tasks"
              value={metrics.totalTasks}
              icon={<Layers size={20} />}
              variant="primary"
            />
            <StatCard
              title="Active Developers"
              value={metrics.activeUsersCount}
              icon={<Users size={20} />}
              variant="purple"
            />
            <StatCard
              title="Completed Tasks"
              value={metrics.completedTasksCount}
              icon={<CheckCircle size={20} />}
              variant="success"
              subtitle={`${metrics.totalTasks > 0 ? Math.round((metrics.completedTasksCount / metrics.totalTasks) * 100) : 0}% completion rate`}
            />
          </div>

          <div className="admin-breakdown-grid">
            <div className="breakdown-card">
              <h3 className="breakdown-title">Status Distribution</h3>
              <div className="breakdown-list">
                {Object.entries(metrics.countsByStatus).map(([status, count]) => (
                  <div key={status} className="breakdown-item">
                    <span className="breakdown-label">{status}</span>
                    <span className="breakdown-bar-container">
                      <div
                        className={`breakdown-bar breakdown-bar--${status.toLowerCase()}`}
                        style={{
                          width: `${metrics.totalTasks > 0 ? (count / metrics.totalTasks) * 100 : 0}%`,
                        }}
                      />
                    </span>
                    <span className="breakdown-count">{count}</span>
                  </div>
                ))}
              </div>
            </div>

            <div className="breakdown-card">
              <h3 className="breakdown-title">Priority Distribution</h3>
              <div className="breakdown-list">
                {Object.entries(metrics.countsByPriority).map(([priority, count]) => (
                  <div key={priority} className="breakdown-item">
                    <span className="breakdown-label">{priority}</span>
                    <span className="breakdown-bar-container">
                      <div
                        className={`breakdown-bar breakdown-bar--priority-${priority.toLowerCase()}`}
                        style={{
                          width: `${metrics.totalTasks > 0 ? (count / metrics.totalTasks) * 100 : 0}%`,
                        }}
                      />
                    </span>
                    <span className="breakdown-count">{count}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
};
