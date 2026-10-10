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
        log.debug("Calculating dashboard metrics concurrently with virtual threads for user '{}'", ownerId);

        LocalDate today = LocalDate.now(clock);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var totalTasksFuture = executor.submit(() -> taskRepository.countByOwnerId(ownerId));
            var statusRecordsFuture = executor.submit(() -> taskRepository.countTasksByStatusForOwner(ownerId));
            var priorityRecordsFuture = executor.submit(() -> taskRepository.countTasksByPriorityForOwner(ownerId));
            var dueTodayFuture = executor.submit(() -> taskRepository.countByOwnerIdAndDueDate(ownerId, today));
            var overdueFuture = executor.submit(() ->
                taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(ownerId, today, TaskStatus.COMPLETED));

            long totalTasks = totalTasksFuture.get();
            var statusRecords = statusRecordsFuture.get();
            var priorityRecords = priorityRecordsFuture.get();
            long dueToday = dueTodayFuture.get();
            long overdue = overdueFuture.get();

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
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dashboard metrics computation interrupted", ie);
        } catch (java.util.concurrent.ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Failed to compute dashboard metrics", cause);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SystemMetricsResponse getSystemMetrics() {
        log.info("Calculating system-level administrative task metrics concurrently by '{}'", SecurityUtils.getCurrentUsername());

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var totalTasksFuture = executor.submit(() -> taskRepository.count());
            var activeUsersFuture = executor.submit(() -> taskRepository.countDistinctOwners());
            var statusRecordsFuture = executor.submit(() -> taskRepository.countTasksByStatusSystemWide());
            var priorityRecordsFuture = executor.submit(() -> taskRepository.countTasksByPrioritySystemWide());

            long totalTasks = totalTasksFuture.get();
            long activeUsers = activeUsersFuture.get();
            var statusRecords = statusRecordsFuture.get();
            var priorityRecords = priorityRecordsFuture.get();

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
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("System metrics computation interrupted", ie);
        } catch (java.util.concurrent.ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Failed to compute system metrics", cause);
        }
    }
}
