package com.devtrack.service;

import com.devtrack.dto.DashboardStatsResponse;
import com.devtrack.dto.ErrorResponse;
import com.devtrack.dto.PriorityCountRecord;
import com.devtrack.dto.StatusCountRecord;
import com.devtrack.dto.SystemMetricsResponse;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.exception.DevTrackException;
import com.devtrack.exception.GlobalExceptionHandler;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Java 21 Virtual Threads & Sealed Hierarchy Verification Tests")
class VirtualThreadConcurrencyTest {

    @Mock
    private TaskRepository taskRepository;

    private DashboardServiceImpl dashboardService;
    private GlobalExceptionHandler exceptionHandler;
    private final String userId = "user-vt-123";

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(taskRepository, Clock.systemUTC());
        exceptionHandler = new GlobalExceptionHandler();

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
    @DisplayName("Java 21 Virtual Thread runtime executes tasks on virtual threads")
    void virtualThreadRuntimeExecutesOnVirtualThread() throws ExecutionException, InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> isVirtualFuture = executor.submit(() -> Thread.currentThread().isVirtual());
            assertTrue(isVirtualFuture.get(), "Task must execute on a Java 21 Virtual Thread");
        }
    }

    @Test
    @DisplayName("DashboardServiceImpl computes user dashboard stats within transactional boundary")
    void dashboardMetricsExecuteWithinTransaction() {
        when(taskRepository.countByOwnerId(userId)).thenReturn(10L);
        when(taskRepository.countTasksByStatusForOwner(userId))
            .thenReturn(List.of(new StatusCountRecord(TaskStatus.OPEN, 6L), new StatusCountRecord(TaskStatus.COMPLETED, 4L)));
        when(taskRepository.countTasksByPriorityForOwner(userId))
            .thenReturn(List.of(new PriorityCountRecord(TaskPriority.HIGH, 7L), new PriorityCountRecord(TaskPriority.LOW, 3L)));
        when(taskRepository.countByOwnerIdAndDueDate(eq(userId), any(LocalDate.class))).thenReturn(2L);
        when(taskRepository.countByOwnerIdAndDueDateBeforeAndStatusNot(eq(userId), any(LocalDate.class), eq(TaskStatus.COMPLETED))).thenReturn(1L);

        DashboardStatsResponse stats = dashboardService.getUserDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.totalTasks());
        assertEquals(2L, stats.dueTodayCount());
        assertEquals(1L, stats.overdueCount());
        assertEquals(6L, stats.countsByStatus().get(TaskStatus.OPEN));
        assertEquals(4L, stats.countsByStatus().get(TaskStatus.COMPLETED));
        assertEquals(7L, stats.countsByPriority().get(TaskPriority.HIGH));
        assertEquals(3L, stats.countsByPriority().get(TaskPriority.LOW));
    }

    @Test
    @DisplayName("DashboardServiceImpl computes system metrics with complete status and priority aggregations")
    void systemMetricsComputeCorrectly() {
        when(taskRepository.count()).thenReturn(50L);
        when(taskRepository.countDistinctOwners()).thenReturn(8L);
        when(taskRepository.countTasksByStatusSystemWide())
            .thenReturn(List.of(new StatusCountRecord(TaskStatus.COMPLETED, 20L), new StatusCountRecord(TaskStatus.OPEN, 30L)));
        when(taskRepository.countTasksByPrioritySystemWide())
            .thenReturn(List.of(new PriorityCountRecord(TaskPriority.CRITICAL, 5L), new PriorityCountRecord(TaskPriority.MEDIUM, 45L)));

        SystemMetricsResponse metrics = dashboardService.getSystemMetrics();

        assertNotNull(metrics);
        assertEquals(50L, metrics.totalTasks());
        assertEquals(8L, metrics.activeUsersCount());
        assertEquals(20L, metrics.completedTasksCount());
        assertEquals(30L, metrics.countsByStatus().get(TaskStatus.OPEN));
        assertEquals(20L, metrics.countsByStatus().get(TaskStatus.COMPLETED));
        assertEquals(5L, metrics.countsByPriority().get(TaskPriority.CRITICAL));
        assertEquals(45L, metrics.countsByPriority().get(TaskPriority.MEDIUM));
    }

    @Test
    @DisplayName("GlobalExceptionHandler maps sealed DevTrackException types to correct HTTP statuses via pattern matching")
    void globalExceptionHandlerMapsSealedHierarchy() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/tasks/test-task-id");

        // 1. ResourceNotFoundException -> 404 Not Found
        UUID taskId = UUID.randomUUID();
        DevTrackException notFound = new ResourceNotFoundException("Task", taskId);
        ResponseEntity<ErrorResponse> notFoundResp = exceptionHandler.handleDevTrackException(notFound, request);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResp.getStatusCode());
        assertNotNull(notFoundResp.getBody());
        assertEquals(404, notFoundResp.getBody().status());
        assertEquals("Not Found", notFoundResp.getBody().error());
        assertEquals(notFound.getMessage(), notFoundResp.getBody().message());
        assertEquals("/api/tasks/test-task-id", notFoundResp.getBody().path());

        // 2. TaskOwnershipException -> 403 Forbidden
        DevTrackException forbidden = new TaskOwnershipException("User does not own task");
        ResponseEntity<ErrorResponse> forbiddenResp = exceptionHandler.handleDevTrackException(forbidden, request);
        assertEquals(HttpStatus.FORBIDDEN, forbiddenResp.getStatusCode());
        assertNotNull(forbiddenResp.getBody());
        assertEquals(403, forbiddenResp.getBody().status());
        assertEquals("Forbidden", forbiddenResp.getBody().error());
        assertEquals("User does not own task", forbiddenResp.getBody().message());

        // 3. InvalidStatusTransitionException -> 400 Bad Request
        DevTrackException invalidStatus = new InvalidStatusTransitionException(TaskStatus.OPEN, TaskStatus.COMPLETED, List.of(TaskStatus.IN_PROGRESS));
        ResponseEntity<ErrorResponse> invalidStatusResp = exceptionHandler.handleDevTrackException(invalidStatus, request);
        assertEquals(HttpStatus.BAD_REQUEST, invalidStatusResp.getStatusCode());
        assertNotNull(invalidStatusResp.getBody());
        assertEquals(400, invalidStatusResp.getBody().status());
        assertEquals("Invalid Status Transition", invalidStatusResp.getBody().error());
        assertEquals(invalidStatus.getMessage(), invalidStatusResp.getBody().message());
    }
}
