package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.util.Map;

public record DashboardStatsResponse(
    long totalTasks,
    Map<TaskStatus, Long> countsByStatus,
    Map<TaskPriority, Long> countsByPriority,
    long dueTodayCount,
    long overdueCount
) {}
