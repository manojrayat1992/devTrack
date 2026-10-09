package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.SystemMetricsResponse;

public interface DashboardService {

    DashboardStatsResponse getUserDashboardStats();

    SystemMetricsResponse getSystemMetrics();
}
