package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.PriorityCountRecord;
import com.devtrack.dto.StatusCountRecord;
import com.devtrack.dto.SystemMetricsResponse;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.repository.TaskRepository;
import com.devtrack.security.SecurityUtils;
import java.time.Clock;
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
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public DashboardServiceImpl(TaskRepository taskRepository, Clock clock) {
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getUserDashboardStats() {
        String ownerId = SecurityUtils.getCurrentUserId();
        log.debug("Calculating dashboard metrics for user '{}'", ownerId);

        LocalDate today = LocalDate.now(clock);

        long totalTasks = taskRepository.countByOwnerId(ownerId);
        java.util.List<StatusCountRecord> statusRecords = taskRepository.countTasksByStatusForOwner(ownerId);
        java.util.List<PriorityCountRecord> priorityRecords = taskRepository.countTasksByPriorityForOwner(ownerId);
        long dueToday = taskRepository.countByOwnerIdAndDueDate(ownerId, today);
        long overdue = taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(ownerId, today, TaskStatus.COMPLETED);

        // Aggregate status counts
        Map<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusCounts.put(status, 0L);
        }
        for (StatusCountRecord record : statusRecords) {
            statusCounts.put(record.status(), record.count());
        }

        // Aggregate priority counts
        Map<TaskPriority, Long> priorityCounts = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            priorityCounts.put(priority, 0L);
        }
        for (PriorityCountRecord record : priorityRecords) {
            priorityCounts.put(record.priority(), record.count());
        }

        return new DashboardStatsResponse(totalTasks, statusCounts, priorityCounts, dueToday, overdue);
    }

    @Override
    @Transactional(readOnly = true)
    public SystemMetricsResponse getSystemMetrics() {
        log.info("Calculating system-level administrative task metrics by '{}'", SecurityUtils.getCurrentUsername());

        long totalTasks = taskRepository.count();
        long activeUsers = taskRepository.countDistinctOwners();
        java.util.List<StatusCountRecord> statusRecords = taskRepository.countTasksByStatusSystemWide();
        java.util.List<PriorityCountRecord> priorityRecords = taskRepository.countTasksByPrioritySystemWide();

        // Single aggregation query for all statuses across system
        Map<TaskStatus, Long> statusCounts = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            statusCounts.put(status, 0L);
        }
        for (StatusCountRecord record : statusRecords) {
            statusCounts.put(record.status(), record.count());
        }

        // Single aggregation query for all priorities across system
        Map<TaskPriority, Long> priorityCounts = new EnumMap<>(TaskPriority.class);
        for (TaskPriority priority : TaskPriority.values()) {
            priorityCounts.put(priority, 0L);
        }
        for (PriorityCountRecord record : priorityRecords) {
            priorityCounts.put(record.priority(), record.count());
        }

        long completed = statusCounts.getOrDefault(TaskStatus.COMPLETED, 0L);

        return new SystemMetricsResponse(totalTasks, activeUsers, statusCounts, priorityCounts, completed);
    }
}
