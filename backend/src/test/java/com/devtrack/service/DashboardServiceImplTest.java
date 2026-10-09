package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;

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
        dashboardService = new DashboardServiceImpl(taskRepository);

        // Configure security context with real SecurityContextImpl and JwtAuthenticationToken
        org.springframework.security.oauth2.jwt.Jwt jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("sub", currentUserId)
            .claim("preferred_username", "alice")
            .build();
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken authentication =
            new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken(jwt, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.context.SecurityContextImpl securityContext = new org.springframework.security.core.context.SecurityContextImpl(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getUserDashboardStats computes user level metrics")
    void getUserDashboardStats() {
        when(taskRepository.countByOwnerId(currentUserId)).thenReturn(10L);
        when(taskRepository.countByOwnerIdAndStatus(currentUserId, TaskStatus.OPEN)).thenReturn(4L);
        when(taskRepository.countByOwnerIdAndStatus(currentUserId, TaskStatus.IN_PROGRESS)).thenReturn(3L);
        when(taskRepository.countByOwnerIdAndStatus(currentUserId, TaskStatus.IN_REVIEW)).thenReturn(1L);
        when(taskRepository.countByOwnerIdAndStatus(currentUserId, TaskStatus.COMPLETED)).thenReturn(2L);
        when(taskRepository.countByOwnerIdAndDueDate(eq(currentUserId), any(LocalDate.class))).thenReturn(1L);
        when(taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(eq(currentUserId), any(LocalDate.class), eq(TaskStatus.COMPLETED))).thenReturn(2L);

        DashboardStatsResponse stats = dashboardService.getUserDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.totalTasks());
        assertEquals(4L, stats.countsByStatus().get(TaskStatus.OPEN));
        assertEquals(2L, stats.countsByStatus().get(TaskStatus.COMPLETED));
        assertEquals(1L, stats.dueTodayCount());
        assertEquals(2L, stats.overdueCount());
    }

    @Test
    @DisplayName("getSystemMetrics aggregates metrics across all users for admin")
    void getSystemMetrics() {
        when(taskRepository.count()).thenReturn(100L);
        when(taskRepository.countDistinctOwners()).thenReturn(15L);
        when(taskRepository.countByStatus(any(TaskStatus.class))).thenReturn(25L);
        when(taskRepository.countByPriority(any(TaskPriority.class))).thenReturn(25L);

        SystemMetricsResponse metrics = dashboardService.getSystemMetrics();

        assertNotNull(metrics);
        assertEquals(100L, metrics.totalTasks());
        assertEquals(15L, metrics.activeUsersCount());
        assertEquals(25L, metrics.completedTasksCount());
    }
}
