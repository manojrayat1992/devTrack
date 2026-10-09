package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.SystemMetricsResponse;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.repository.TaskRepository;
import com.devtrack.security.SecurityUtils;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final TaskRepository taskRepository;

    public DashboardServiceImpl(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getUserDashboardStats() {
        String ownerId = SecurityUtils.getCurrentUserId();
        log.debug("Calculating dashboard metrics for user '{}'", ownerId);

        long totalTasks = taskRepository.countByOwnerId(ownerId);

        Map<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusCounts.put(status, taskRepository.countByOwnerIdAndStatus(ownerId, status));
        }

        Map<TaskPriority, Long> priorityCounts = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            priorityCounts.put(priority, taskRepository.countByOwnerIdAndPriority(ownerId, priority));
        }

        LocalDate today = LocalDate.now();
        long dueToday = taskRepository.countByOwnerIdAndDueDate(ownerId, today);
        long overdue = taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(
            ownerId, today, TaskStatus.COMPLETED
        );

        return new DashboardStatsResponse(totalTasks, statusCounts, priorityCounts, dueToday, overdue);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemMetricsResponse getSystemMetrics() {
        log.info("Calculating system-level administrative task metrics by '{}'", SecurityUtils.getCurrentUsername());

        long totalTasks = taskRepository.count();
        long activeUsers = taskRepository.countDistinctOwners();

        Map<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusCounts.put(status, taskRepository.countByStatus(status));
        }

        Map<TaskPriority, Long> priorityCounts = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            priorityCounts.put(priority, taskRepository.countByPriority(priority));
        }

        long completed = statusCounts.getOrDefault(TaskStatus.COMPLETED, 0L);

        return new SystemMetricsResponse(totalTasks, activeUsers, statusCounts, priorityCounts, completed);
    }
}
