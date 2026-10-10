package com.devtrack.service;

import com.devtrack.dto.PageResponse;
import com.devtrack.dto.TaskCreateRequest;
import com.devtrack.dto.TaskFilterCriteria;
import com.devtrack.dto.TaskResponse;
import com.devtrack.dto.TaskStatusUpdateRequest;
import com.devtrack.dto.TaskUpdateRequest;
import com.devtrack.entity.Task;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.exception.InvalidStatusTransitionException;
import com.devtrack.exception.ResourceNotFoundException;
import com.devtrack.exception.TaskOwnershipException;
import com.devtrack.mapper.TaskMapper;
import com.devtrack.repository.TaskRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl Business Logic Tests")
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    private TaskStatusTransitionEngine transitionEngine;
    private TaskMapper taskMapper;
    private TaskServiceImpl taskService;

    private final String currentUserId = "user-alice-123";

    @BeforeEach
    void setUp() {
        transitionEngine = new TaskStatusTransitionEngine();
        taskMapper = new TaskMapper(transitionEngine);
        taskService = new TaskServiceImpl(taskRepository, taskMapper, transitionEngine);

        // Configure security context with real SecurityContextImpl and JwtAuthenticationToken
        Jwt jwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("sub", currentUserId)
            .claim("preferred_username", "alice")
            .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
        org.springframework.security.core.context.SecurityContextImpl securityContext = new org.springframework.security.core.context.SecurityContextImpl(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("createTask assigns authenticated owner ID and saves task")
    void createTaskSuccess() {
        TaskCreateRequest request = new TaskCreateRequest(
            "Write integration test",
            "Details about test",
            TaskPriority.HIGH,
            TaskStatus.OPEN,
            LocalDate.now().plusDays(2)
        );

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            t.setCreatedAt(Instant.now());
            t.setUpdatedAt(Instant.now());
            return t;
        });

        TaskResponse response = taskService.createTask(request);

        assertNotNull(response);
        assertEquals("Write integration test", response.title());
        assertEquals(TaskPriority.HIGH, response.priority());
        assertEquals(TaskStatus.OPEN, response.status());
        assertEquals(currentUserId, response.ownerId());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("getTaskById returns task when user is the owner")
    void getTaskByIdSuccess() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc", TaskPriority.MEDIUM, TaskStatus.OPEN, null, currentUserId);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTaskById(taskId);

        assertNotNull(response);
        assertEquals(taskId, response.id());
        assertEquals(currentUserId, response.ownerId());
    }

    @Test
    @DisplayName("getTaskById throws TaskOwnershipException when user does not own the task")
    void getTaskByIdForbiddenForNonOwner() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Secret Task", "Desc", TaskPriority.HIGH, TaskStatus.OPEN, null, "user-bob-999");

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        TaskOwnershipException exception = assertThrows(
            TaskOwnershipException.class,
            () -> taskService.getTaskById(taskId)
        );

        assertTrue(exception.getMessage().contains("not authorized"));
    }

    @Test
    @DisplayName("getTaskById throws TaskOwnershipException even if current user has ROLE_ADMIN")
    void adminCannotAccessAnotherUsersPrivateTask() {
        Jwt adminJwt = Jwt.withTokenValue("mock-admin-token")
            .header("alg", "none")
            .claim("sub", "admin-sarah-456")
            .claim("preferred_username", "sarah_admin")
            .build();
        JwtAuthenticationToken adminAuth = new JwtAuthenticationToken(
            adminJwt,
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.setContext(new org.springframework.security.core.context.SecurityContextImpl(adminAuth));

        UUID taskId = UUID.randomUUID();
        Task userTask = new Task(taskId, "User Private Task", "Desc", TaskPriority.HIGH, TaskStatus.OPEN, null, "user-bob-999");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(userTask));

        TaskOwnershipException exception = assertThrows(
            TaskOwnershipException.class,
            () -> taskService.getTaskById(taskId)
        );

        assertTrue(exception.getMessage().contains("not authorized"));
    }

    @Test
    @DisplayName("getTaskById throws ResourceNotFoundException when task does not exist")
    void getTaskByIdNotFound() {
        UUID taskId = UUID.randomUUID();
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> taskService.getTaskById(taskId));
    }

    @Test
    @DisplayName("updateTaskStatus validates transition and updates status")
    void updateTaskStatusSuccess() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc", TaskPriority.LOW, TaskStatus.OPEN, null, currentUserId);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse response = taskService.updateTaskStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS));

        assertEquals(TaskStatus.IN_PROGRESS, response.status());
        verify(taskRepository, times(1)).save(task);
    }

    @Test
    @DisplayName("updateTaskStatus throws InvalidStatusTransitionException on illegal transition")
    void updateTaskStatusInvalidTransition() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task 1", "Desc", TaskPriority.LOW, TaskStatus.OPEN, null, currentUserId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThrows(
            InvalidStatusTransitionException.class,
            () -> taskService.updateTaskStatus(taskId, new TaskStatusUpdateRequest(TaskStatus.IN_REVIEW))
        );

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("deleteTask removes task when user is the owner")
    void deleteTaskSuccess() {
        UUID taskId = UUID.randomUUID();
        Task task = new Task(taskId, "Task to delete", "Desc", TaskPriority.LOW, TaskStatus.OPEN, null, currentUserId);

        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskService.deleteTask(taskId);

        verify(taskRepository, times(1)).delete(task);
    }

    @Test
    @DisplayName("findTasks applies filters and returns deterministic page response")
    void findTasksReturnsPage() {
        Task task1 = new Task(UUID.randomUUID(), "T1", "D1", TaskPriority.HIGH, TaskStatus.OPEN, null, currentUserId);
        task1.setCreatedAt(Instant.now());
        task1.setUpdatedAt(Instant.now());

        Page<Task> page = new PageImpl<>(List.of(task1), PageRequest.of(0, 10), 1);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        TaskFilterCriteria criteria = new TaskFilterCriteria(TaskStatus.OPEN, null, null, null, null);
        PageResponse<TaskResponse> response = taskService.findTasks(criteria, PageRequest.of(0, 10));

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
        assertEquals("T1", response.content().get(0).title());
    }

    @Test
    @DisplayName("updateTask throws TaskOwnershipException when user does not own the task")
    void updateTaskForbiddenForNonOwner() {
        UUID taskId = UUID.randomUUID();
        Task foreignTask = new Task(taskId, "Foreign Task", "Desc", TaskPriority.LOW, TaskStatus.OPEN, null, "user-bob-999");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(foreignTask));

        TaskUpdateRequest request = new TaskUpdateRequest("New Title", "New Desc", TaskPriority.HIGH, null);

        assertThrows(TaskOwnershipException.class, () -> taskService.updateTask(taskId, request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("deleteTask throws TaskOwnershipException when user does not own the task")
    void deleteTaskForbiddenForNonOwner() {
        UUID taskId = UUID.randomUUID();
        Task foreignTask = new Task(taskId, "Foreign Task", "Desc", TaskPriority.LOW, TaskStatus.OPEN, null, "user-bob-999");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(foreignTask));

        assertThrows(TaskOwnershipException.class, () -> taskService.deleteTask(taskId));
        verify(taskRepository, never()).delete(any(Task.class));
    }

    @Test
    @DisplayName("updateTaskStatus throws TaskOwnershipException when user does not own the task")
    void updateTaskStatusForbiddenForNonOwner() {
        UUID taskId = UUID.randomUUID();
        Task foreignTask = new Task(taskId, "Foreign Task", "Desc", TaskPriority.LOW, TaskStatus.OPEN, null, "user-bob-999");
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(foreignTask));

        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);

        assertThrows(TaskOwnershipException.class, () -> taskService.updateTaskStatus(taskId, request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("createTask with non-OPEN status throws InvalidStatusTransitionException")
    void createTaskWithNonOpenStatusFails() {
        TaskCreateRequest request = new TaskCreateRequest(
            "Direct Completed Task",
            "Should not be allowed to bypass lifecycle",
            TaskPriority.HIGH,
            TaskStatus.COMPLETED,
            LocalDate.now().plusDays(2)
        );

        assertThrows(InvalidStatusTransitionException.class, () -> taskService.createTask(request));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    @DisplayName("getTaskById without authentication throws AccessDeniedException")
    void unauthenticatedAccessThrowsAccessDeniedException() {
        SecurityContextHolder.clearContext();
        UUID taskId = UUID.randomUUID();

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
            () -> taskService.getTaskById(taskId));
    }
}
