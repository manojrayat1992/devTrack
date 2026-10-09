import { apiClient } from './apiClient';
import { DashboardStatsResponse, SystemMetricsResponse } from '../types';

export const dashboardApi = {
  getUserDashboardStats: async (): Promise<DashboardStatsResponse> => {
    const response = await apiClient.get<DashboardStatsResponse>('/dashboard');
    return response.data;
  },

  getSystemMetrics: async (): Promise<SystemMetricsResponse> => {
    const response = await apiClient.get<SystemMetricsResponse>('/admin/metrics');
    return response.data;
  },
};
