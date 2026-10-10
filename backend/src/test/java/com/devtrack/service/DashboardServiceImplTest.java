package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.PriorityCountRecord;
import com.devtrack.dto.StatusCountRecord;
import com.devtrack.dto.SystemMetricsResponse;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Tests")
class DashboardServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    private DashboardServiceImpl dashboardService;
    private final String currentUserId = "user-alice-123";

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(taskRepository, Clock.systemUTC());

        // Configure security context with real SecurityContextImpl and JwtAuthenticationToken
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("sub", currentUserId)
            .claim("preferred_username", "alice")
            .build();
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken authentication =
            new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.context.SecurityContextImpl securityContext = new org.springframework.security.core.context.SecurityContextImpl(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getUserDashboardStats computes user level metrics via optimized aggregation queries")
    void getUserDashboardStats() {
        when(taskRepository.countByOwnerId(currentUserId)).thenReturn(10L);
        when(taskRepository.countTasksByStatusForOwner(currentUserId)).thenReturn(List.of(
            new StatusCountRecord(TaskStatus.OPEN, 4L),
            new StatusCountRecord(TaskStatus.IN_PROGRESS, 3L),
            new StatusCountRecord(TaskStatus.IN_REVIEW, 1L),
            new StatusCountRecord(TaskStatus.COMPLETED, 2L)
        ));
        when(taskRepository.countTasksByPriorityForOwner(currentUserId)).thenReturn(List.of(
            new PriorityCountRecord(TaskPriority.HIGH, 5L),
            new PriorityCountRecord(TaskPriority.MEDIUM, 5L)
        ));
        when(taskRepository.countByOwnerIdAndDueDate(eq(currentUserId), any(LocalDate.class))).thenReturn(1L);
        when(taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(eq(currentUserId), any(LocalDate.class), eq(TaskStatus.COMPLETED))).thenReturn(2L);

        DashboardStatsResponse stats = dashboardService.getUserDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.totalTasks());
        assertEquals(4L, stats.countsByStatus().get(TaskStatus.OPEN));
        assertEquals(2L, stats.countsByStatus().get(TaskStatus.COMPLETED));
        assertEquals(1L, stats.dueTodayCount());
        assertEquals(2L, stats.overdueCount());

        verify(taskRepository, times(1)).countTasksByStatusForOwner(currentUserId);
        verify(taskRepository, times(1)).countTasksByPriorityForOwner(currentUserId);
    }

    @Test
    @DisplayName("getSystemMetrics aggregates metrics across all users for admin")
    void getSystemMetrics() {
        when(taskRepository.count()).thenReturn(100L);
        when(taskRepository.countDistinctOwners()).thenReturn(15L);
        when(taskRepository.countTasksByStatusSystemWide()).thenReturn(List.of(
            new StatusCountRecord(TaskStatus.COMPLETED, 25L),
            new StatusCountRecord(TaskStatus.OPEN, 25L),
            new StatusCountRecord(TaskStatus.IN_PROGRESS, 25L),
            new StatusCountRecord(TaskStatus.IN_REVIEW, 25L)
        ));
        when(taskRepository.countTasksByPrioritySystemWide()).thenReturn(List.of(
            new PriorityCountRecord(TaskPriority.HIGH, 50L),
            new PriorityCountRecord(TaskPriority.MEDIUM, 50L)
        ));

        SystemMetricsResponse metrics = dashboardService.getSystemMetrics();

        assertNotNull(metrics);
        assertEquals(100L, metrics.totalTasks());
        assertEquals(15L, metrics.activeUsersCount());
        assertEquals(25L, metrics.completedTasksCount());

        verify(taskRepository, times(1)).countTasksByStatusSystemWide();
        verify(taskRepository, times(1)).countTasksByPrioritySystemWide();
    }
}
