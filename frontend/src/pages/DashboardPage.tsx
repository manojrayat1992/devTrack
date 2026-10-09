import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { taskApi } from '../api/taskApi';
import { dashboardApi } from '../api/dashboardApi';
import {
  Task,
  TaskCreateRequest,
  TaskFilterCriteria,
  TaskStatus,
  TaskUpdateRequest,
  DashboardStatsResponse,
} from '../types';
import { StatCard } from '../components/StatCard';
import { TaskCard } from '../components/TaskCard';
import { TaskModal } from '../components/TaskModal';
import { TaskFilterBar } from '../components/TaskFilterBar';
import { Pagination } from '../components/Pagination';
import {
  Plus,
  ListTodo,
  Clock,
  Code,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  FolderOpen,
  LogIn,
} from 'lucide-react';
import './DashboardPage.scss';

export const DashboardPage: React.FC = () => {
  const { authenticated, loading: authLoading, login, authError } = useAuth();

  const [stats, setStats] = useState<DashboardStatsResponse | null>(null);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(0);
  const [page, setPage] = useState<number>(0);
  const [pageSize, setPageSize] = useState<number>(10);

  const [criteria, setCriteria] = useState<TaskFilterCriteria>({});
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [taskToEdit, setTaskToEdit] = useState<Task | null>(null);
  const [transitioningTaskId, setTransitioningTaskId] = useState<string | null>(null);

  const fetchStats = useCallback(async () => {
    if (!authenticated) return;
    try {
      const data = await dashboardApi.getUserDashboardStats();
      setStats(data);
    } catch (err: unknown) {
      console.warn('Could not fetch user stats:', err);
    }
  }, [authenticated]);

  const fetchTasks = useCallback(async () => {
    if (!authenticated) return;
    try {
      setLoading(true);
      setError(null);
      const data = await taskApi.getTasks(criteria, page, pageSize, 'createdAt,desc');
      setTasks(data.content);
      setTotalElements(data.totalElements);
      setTotalPages(data.totalPages);
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      setError(errObj.message || 'Failed to fetch tasks.');
    } finally {
      setLoading(false);
    }
  }, [authenticated, criteria, page, pageSize]);

  useEffect(() => {
    if (authenticated) {
      fetchStats();
      fetchTasks();
    }
  }, [authenticated, fetchStats, fetchTasks]);

  const handleCreateOrUpdate = async (data: TaskCreateRequest | TaskUpdateRequest) => {
    if (taskToEdit) {
      await taskApi.updateTask(taskToEdit.id, data as TaskUpdateRequest);
    } else {
      await taskApi.createTask(data as TaskCreateRequest);
    }
    fetchStats();
    fetchTasks();
  };

  const handleDelete = async (taskId: string) => {
    if (window.confirm('Are you sure you want to delete this task?')) {
      try {
        await taskApi.deleteTask(taskId);
        fetchStats();
        fetchTasks();
      } catch (err: unknown) {
        const errObj = err as { message?: string };
        alert(errObj.message || 'Failed to delete task');
      }
    }
  };

  const handleStatusTransition = async (taskId: string, newStatus: TaskStatus) => {
    try {
      setTransitioningTaskId(taskId);
      await taskApi.updateTaskStatus(taskId, newStatus);
      fetchStats();
      fetchTasks();
    } catch (err: unknown) {
      const errObj = err as { message?: string };
      alert(errObj.message || 'Failed to update task status');
    } finally {
      setTransitioningTaskId(null);
    }
  };

  const handleFilterStatClick = (statusFilter?: TaskStatus) => {
    setCriteria((prev) => ({
      ...prev,
      status: prev.status === statusFilter ? '' : statusFilter,
    }));
    setPage(0);
  };

  if (authLoading) {
    return (
      <div className="dashboard-state">
        <RefreshCw size={36} className="spin-icon" />
        <p>Connecting to DevTrack authentication server...</p>
      </div>
    );
  }

  if (!authenticated) {
    return (
      <div className="dashboard-unauthenticated">
        <div className="unauth-card">
          <div className="unauth-card__icon">
            <LogIn size={40} />
          </div>
          <h1 className="unauth-card__title">Welcome to DevTrack</h1>
          <p className="unauth-card__subtitle">
            Enterprise Developer Task Management with Keycloak OpenID Connect authentication.
          </p>

          {authError && (
            <div className="unauth-card__warning">
              <AlertTriangle size={16} />
              <span>{authError}</span>
            </div>
          )}

          <button type="button" onClick={login} className="btn-login-hero">
            <LogIn size={18} />
            Sign In with Keycloak
          </button>

          <div className="unauth-card__credentials">
            <strong>Development Credentials:</strong>
            <p>Developer: <code>dev_user</code> / <code>password123</code></p>
            <p>Administrator: <code>dev_admin</code> / <code>password123</code></p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-page">
      <div className="dashboard-header">
        <div>
          <h1 className="dashboard-title">Developer Workspace</h1>
          <p className="dashboard-subtitle">
            Manage your sprint tasks, track progress, and trigger validated status transitions.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            setTaskToEdit(null);
            setIsModalOpen(true);
          }}
          className="btn-create"
        >
          <Plus size={18} />
          <span>New Task</span>
        </button>
      </div>

      {stats && (
        <div className="dashboard-stats-grid">
          <StatCard
            title="Total Tasks"
            value={stats.totalTasks}
            icon={<ListTodo size={20} />}
            variant="primary"
            onClick={() => handleFilterStatClick(undefined)}
            active={!criteria.status}
          />
          <StatCard
            title="Open"
            value={stats.countsByStatus.OPEN || 0}
            icon={<Clock size={20} />}
            variant="neutral"
            onClick={() => handleFilterStatClick('OPEN')}
            active={criteria.status === 'OPEN'}
          />
          <StatCard
            title="In Progress"
            value={stats.countsByStatus.IN_PROGRESS || 0}
            icon={<Code size={20} />}
            variant="primary"
            onClick={() => handleFilterStatClick('IN_PROGRESS')}
            active={criteria.status === 'IN_PROGRESS'}
          />
          <StatCard
            title="In Review"
            value={stats.countsByStatus.IN_REVIEW || 0}
            icon={<RefreshCw size={20} />}
            variant="purple"
            onClick={() => handleFilterStatClick('IN_REVIEW')}
            active={criteria.status === 'IN_REVIEW'}
          />
          <StatCard
            title="Completed"
            value={stats.countsByStatus.COMPLETED || 0}
            icon={<CheckCircle2 size={20} />}
            variant="success"
            onClick={() => handleFilterStatClick('COMPLETED')}
            active={criteria.status === 'COMPLETED'}
          />
          {stats.overdueCount > 0 && (
            <StatCard
              title="Overdue"
              value={stats.overdueCount}
              icon={<AlertTriangle size={20} />}
              variant="danger"
            />
          )}
        </div>
      )}

      <div className="dashboard-toolbar">
        <TaskFilterBar
          criteria={criteria}
          onChange={(newCriteria) => {
            setCriteria(newCriteria);
            setPage(0);
          }}
          onReset={() => {
            setCriteria({});
            setPage(0);
          }}
        />
      </div>

      {error && (
        <div className="dashboard-error">
          <AlertTriangle size={18} />
          <span>{error}</span>
          <button type="button" onClick={fetchTasks} className="btn-retry">
            Retry
          </button>
        </div>
      )}

      {loading ? (
        <div className="dashboard-loading">
          <RefreshCw size={32} className="spin-icon" />
          <span>Loading tasks...</span>
        </div>
      ) : tasks.length === 0 ? (
        <div className="dashboard-empty">
          <FolderOpen size={48} className="empty-icon" />
          <h3>No tasks found</h3>
          <p>
            {Object.keys(criteria).length > 0
              ? 'Try modifying your search or filter options.'
              : 'You have no tasks yet. Create your first task to get started.'}
          </p>
          <button
            type="button"
            onClick={() => {
              setTaskToEdit(null);
              setIsModalOpen(true);
            }}
            className="btn-create-empty"
          >
            <Plus size={16} />
            Create Task
          </button>
        </div>
      ) : (
        <>
          <div className="tasks-grid">
            {tasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                onEdit={(t) => {
                  setTaskToEdit(t);
                  setIsModalOpen(true);
                }}
                onDelete={handleDelete}
                onStatusTransition={handleStatusTransition}
                isTransitioning={transitioningTaskId === task.id}
              />
            ))}
          </div>

          <Pagination
            currentPage={page}
            totalPages={totalPages}
            totalElements={totalElements}
            pageSize={pageSize}
            onPageChange={(p) => setPage(p)}
            onPageSizeChange={(s) => {
              setPageSize(s);
              setPage(0);
            }}
          />
        </>
      )}

      <TaskModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setTaskToEdit(null);
        }}
        onSubmit={handleCreateOrUpdate}
        taskToEdit={taskToEdit}
      />
    </div>
  );
};
