import { apiClient } from './apiClient';
import { PageResponse, Task, TaskCreateRequest, TaskFilterCriteria, TaskStatus, TaskUpdateRequest } from '../types';

export const taskApi = {
  getTasks: async (
    criteria: TaskFilterCriteria = {},
    page: number = 0,
    size: number = 10,
    sort: string = 'createdAt,desc'
  ): Promise<PageResponse<Task>> => {
    const params: Record<string, string | number> = {
      page,
      size,
      sort,
    };

    if (criteria.status) params.status = criteria.status;
    if (criteria.priority) params.priority = criteria.priority;
    if (criteria.dueBefore) params.dueBefore = criteria.dueBefore;
    if (criteria.dueAfter) params.dueAfter = criteria.dueAfter;
    if (criteria.search) params.search = criteria.search;

    const response = await apiClient.get<PageResponse<Task>>('/tasks', { params });
    return response.data;
  },

  getTaskById: async (id: string): Promise<Task> => {
    const response = await apiClient.get<Task>(`/tasks/${id}`);
    return response.data;
  },

  createTask: async (data: TaskCreateRequest): Promise<Task> => {
    const response = await apiClient.post<Task>('/tasks', data);
    return response.data;
  },

  updateTask: async (id: string, data: TaskUpdateRequest): Promise<Task> => {
    const response = await apiClient.put<Task>(`/tasks/${id}`, data);
    return response.data;
  },

  updateTaskStatus: async (id: string, status: TaskStatus): Promise<Task> => {
    const response = await apiClient.patch<Task>(`/tasks/${id}/status`, { status });
    return response.data;
  },

  deleteTask: async (id: string): Promise<void> => {
    await apiClient.delete(`/tasks/${id}`);
  },
};
