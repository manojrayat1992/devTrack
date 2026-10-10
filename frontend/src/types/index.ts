export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export type TaskStatus = 'OPEN' | 'IN_PROGRESS' | 'IN_REVIEW' | 'COMPLETED';

export interface Task {
  id: string;
  title: string;
  description?: string | null;
  priority: TaskPriority;
  status: TaskStatus;
  dueDate?: string | null;
  ownerId: string;
  createdAt: string;
  updatedAt: string;
  allowedNextStatuses: TaskStatus[];
  version?: number;
}

export interface TaskCreateRequest {
  title: string;
  description?: string;
  priority: TaskPriority;
  status?: TaskStatus;
  dueDate?: string;
}

export interface TaskUpdateRequest {
  title: string;
  description?: string;
  priority: TaskPriority;
  dueDate?: string;
}

export interface TaskFilterCriteria {
  status?: TaskStatus | '';
  priority?: TaskPriority | '';
  dueBefore?: string;
  dueAfter?: string;
  search?: string;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface DashboardStatsResponse {
  totalTasks: number;
  countsByStatus: Record<TaskStatus, number>;
  countsByPriority: Record<TaskPriority, number>;
  dueTodayCount: number;
  overdueCount: number;
}

export interface SystemMetricsResponse {
  totalTasks: number;
  activeUsersCount: number;
  countsByStatus: Record<TaskStatus, number>;
  countsByPriority: Record<TaskPriority, number>;
  completedTasksCount: number;
}

export interface UserProfile {
  id: string;
  username: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles: string[];
  isAdmin: boolean;
}
