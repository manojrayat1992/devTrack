package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.util.Map;

public record SystemMetricsResponse(
    long totalTasks,
    long activeUsersCount,
    Map<TaskStatus, Long> countsByStatus,
    Map<TaskPriority, Long> countsByPriority,
    long completedTasksCount
) {}
