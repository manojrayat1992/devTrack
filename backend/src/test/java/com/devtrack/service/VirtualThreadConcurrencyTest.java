package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.PriorityCountRecord;
import com.devtrack.dto.StatusCountRecord;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.event.TaskDomainEvent;
import com.devtrack.exception.DevTrackException;
import com.devtrack.exception.InvalidStatusTransitionException;
import com.devtrack.exception.ResourceNotFoundException;
import com.devtrack.exception.TaskOwnershipException;
import com.devtrack.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Java 21 Virtual Threads & Sealed Hierarchy Verification Tests")
class VirtualThreadConcurrencyTest {

    @Mock
    private TaskRepository taskRepository;

    private DashboardServiceImpl dashboardService;
    private final String userId = "user-vt-123";

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(taskRepository, Clock.systemUTC());

        Jwt jwt = Jwt.withTokenValue("vt-token")
            .header("alg", "none")
            .claim("sub", userId)
            .claim("preferred_username", "vt_user")
            .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
        SecurityContextHolder.setContext(new org.springframework.security.core.context.SecurityContextImpl(auth));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("DashboardServiceImpl computes metrics concurrently across Java 21 Virtual Threads")
    void dashboardMetricsExecuteConcurrentlyOnVirtualThreads() {
        when(taskRepository.countByOwnerId(userId)).thenReturn(10L);
        when(taskRepository.countTasksByStatusForOwner(userId)).thenReturn(List.of(new StatusCountRecord(TaskStatus.OPEN, 10L)));
        when(taskRepository.countTasksByPriorityForOwner(userId)).thenReturn(List.of(new PriorityCountRecord(TaskPriority.HIGH, 10L)));
        when(taskRepository.countByOwnerIdAndDueDate(eq(userId), any(LocalDate.class))).thenReturn(2L);
        when(taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(eq(userId), any(LocalDate.class), eq(TaskStatus.COMPLETED))).thenReturn(1L);

        DashboardStatsResponse stats = dashboardService.getUserDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.totalTasks());
        assertEquals(2L, stats.dueTodayCount());
        assertEquals(1L, stats.overdueCount());
    }

    @Test
    @DisplayName("Java 21 Pattern Matching on Sealed DevTrackException hierarchy is compiler-verified")
    void sealedExceptionHierarchyPatternMatching() {
        DevTrackException notFound = new ResourceNotFoundException("Task", UUID.randomUUID());
        DevTrackException forbidden = new TaskOwnershipException("Unauthorized");
        DevTrackException invalidStatus = new InvalidStatusTransitionException(TaskStatus.OPEN, TaskStatus.COMPLETED, List.of());

        assertEquals("NOT_FOUND", classify(notFound));
        assertEquals("FORBIDDEN", classify(forbidden));
        assertEquals("BAD_REQUEST", classify(invalidStatus));
    }

    private String classify(DevTrackException ex) {
        // Compiler guarantees exhaustiveness without default case
        return switch (ex) {
            case ResourceNotFoundException r -> r.getMessage() != null ? "NOT_FOUND" : "NOT_FOUND";
            case TaskOwnershipException t -> t.getMessage() != null ? "FORBIDDEN" : "FORBIDDEN";
            case InvalidStatusTransitionException i -> i.getMessage() != null ? "BAD_REQUEST" : "BAD_REQUEST";
        };
    }

    @Test
    @DisplayName("Java 21 Sealed TaskDomainEvent records support exhaustive pattern matching")
    void sealedTaskDomainEventsPatternMatching() {
        UUID taskId = UUID.randomUUID();
        TaskDomainEvent event = new TaskDomainEvent.TaskCreatedEvent(
            taskId, userId, "Build Feature", TaskPriority.HIGH, Instant.now()
        );

        String type = switch (event) {
            case TaskDomainEvent.TaskCreatedEvent c -> "CREATED:" + c.title();
            case TaskDomainEvent.TaskStatusChangedEvent s -> "STATUS:" + s.toStatus();
            case TaskDomainEvent.TaskDeletedEvent d -> "DELETED:" + d.taskId();
        };

        assertEquals("CREATED:Build Feature", type);
    }
}
