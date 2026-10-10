package com.devtrack.controller;

import com.devtrack.dto.PageResponse;
import com.devtrack.dto.TaskCreateRequest;
import com.devtrack.dto.TaskFilterCriteria;
import com.devtrack.dto.TaskResponse;
import com.devtrack.dto.TaskStatusUpdateRequest;
import com.devtrack.dto.TaskUpdateRequest;
import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import com.devtrack.exception.InvalidStatusTransitionException;
import com.devtrack.exception.ResourceNotFoundException;
import com.devtrack.exception.TaskOwnershipException;
import com.devtrack.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("TaskController Web Layer Tests")
class TaskControllerSliceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @Test
    @WithMockUser
    @DisplayName("POST /api/tasks creates task and returns 201 Created")
    void createTaskReturns201() throws Exception {
        UUID id = UUID.randomUUID();
        TaskCreateRequest request = new TaskCreateRequest(
            "Write API documentation",
            "Document all endpoints with OpenAPI",
            TaskPriority.HIGH,
            TaskStatus.OPEN,
            LocalDate.now().plusDays(2)
        );

        TaskResponse response = new TaskResponse(
            id, request.title(), request.description(), request.priority(),
            TaskStatus.OPEN, request.dueDate(), "user-123",
            Instant.now(), Instant.now(), List.of(TaskStatus.IN_PROGRESS)
        );

        when(taskService.createTask(any(TaskCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value("Write API documentation"))
            .andExpect(jsonPath("$.priority").value("HIGH"))
            .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @WithMockUser
    @DisplayName("POST /api/tasks with invalid payload returns 400 Bad Request with validation errors")
    void createTaskInvalidPayloadReturns400() throws Exception {
        TaskCreateRequest invalidRequest = new TaskCreateRequest(
            "", // Blank title
            "Description",
            null, // Null priority
            TaskStatus.OPEN,
            LocalDate.now()
        );

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors.title").exists())
            .andExpect(jsonPath("$.validationErrors.priority").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} returns 200 OK when found")
    void getTaskByIdReturns200() throws Exception {
        UUID id = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
            id, "Task A", "Desc", TaskPriority.LOW,
            TaskStatus.OPEN, null, "user-123",
            Instant.now(), Instant.now(), List.of()
        );

        when(taskService.getTaskById(id)).thenReturn(response);

        mockMvc.perform(get("/api/tasks/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.title").value("Task A"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} returns 404 Not Found when resource does not exist")
    void getTaskByIdNotFoundReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTaskById(id)).thenThrow(new ResourceNotFoundException("Task", id));

        mockMvc.perform(get("/api/tasks/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} returns 403 Forbidden when owned by another user")
    void getTaskByIdForbiddenReturns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTaskById(id)).thenThrow(new TaskOwnershipException("You are not authorized"));

        mockMvc.perform(get("/api/tasks/{id}", id))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/tasks/{id}/status with invalid transition returns 400 Bad Request")
    void patchStatusInvalidTransitionReturns400() throws Exception {
        UUID id = UUID.randomUUID();
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.IN_REVIEW);

        when(taskService.updateTaskStatus(eq(id), any(TaskStatusUpdateRequest.class)))
            .thenThrow(new InvalidStatusTransitionException(TaskStatus.OPEN, TaskStatus.IN_REVIEW, List.of(TaskStatus.IN_PROGRESS)));

        mockMvc.perform(patch("/api/tasks/{id}/status", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("Invalid Status Transition"));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/tasks/{id} returns 204 No Content")
    void deleteTaskReturns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(taskService).deleteTask(id);

        mockMvc.perform(delete("/api/tasks/{id}", id).with(csrf()))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks returns 200 with paginated results")
    void listTasksReturns200() throws Exception {
        PageResponse<TaskResponse> pageResponse = new PageResponse<>(
            List.of(), 0, 10, 0, 0, true, true, false, false
        );

        when(taskService.findTasks(any(TaskFilterCriteria.class), any(Pageable.class)))
            .thenReturn(pageResponse);

        mockMvc.perform(get("/api/tasks")
                .param("status", "OPEN")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(10));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/tasks/{id} with concurrent modification conflict returns 409 Conflict")
    void updateTaskConflictReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        TaskUpdateRequest request = new TaskUpdateRequest(
            "Concurrent Update",
            "Details",
            TaskPriority.HIGH,
            LocalDate.now()
        );

        when(taskService.updateTask(eq(id), any(TaskUpdateRequest.class)))
            .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("Row updated by another transaction"));

        mockMvc.perform(put("/api/tasks/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("The task was modified by another concurrent transaction. Please reload the task and retry your change."));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} with non-UUID path variable returns 400 Bad Request")
    void getTaskInvalidUuidReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", "not-a-valid-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks with invalid sort property returns 400 Bad Request")
    void listTasksInvalidSortPropertyReturns400() throws Exception {
        when(taskService.findTasks(any(TaskFilterCriteria.class), any(Pageable.class)))
            .thenThrow(new org.springframework.data.mapping.PropertyReferenceException("invalidField", org.springframework.data.util.TypeInformation.of(com.devtrack.entity.Task.class), java.util.List.of()));

        mockMvc.perform(get("/api/tasks").param("sort", "invalidField,desc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Invalid sorting or filter property: 'invalidField'"));
    }

    @Test
    @WithMockUser
    @DisplayName("GET /api/tasks/{id} with cross-user ownership violation returns 403 Forbidden")
    void getTaskCrossUserForbiddenReturns403() throws Exception {
        UUID id = UUID.randomUUID();
        when(taskService.getTaskById(id))
            .thenThrow(new TaskOwnershipException("You are not authorized to access or modify this task"));

        mockMvc.perform(get("/api/tasks/{id}", id))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").value("You are not authorized to access or modify this task"));
    }

    @Test
    @WithMockUser
    @DisplayName("PUT /api/tasks/{id} with ownership violation returns 403 Forbidden")
    void updateTaskCrossUserForbiddenReturns403() throws Exception {
        UUID id = UUID.randomUUID();
        TaskUpdateRequest request = new TaskUpdateRequest("Title", "Desc", TaskPriority.HIGH, null);
        when(taskService.updateTask(eq(id), any(TaskUpdateRequest.class)))
            .thenThrow(new TaskOwnershipException("You are not authorized to access or modify this task"));

        mockMvc.perform(put("/api/tasks/{id}", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"))
            .andExpect(jsonPath("$.message").value("You are not authorized to access or modify this task"));
    }

    @Test
    @WithMockUser
    @DisplayName("DELETE /api/tasks/{id} with ownership violation returns 403 Forbidden")
    void deleteTaskCrossUserForbiddenReturns403() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new TaskOwnershipException("You are not authorized to access or modify this task"))
            .when(taskService).deleteTask(id);

        mockMvc.perform(delete("/api/tasks/{id}", id).with(csrf()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser
    @DisplayName("PATCH /api/tasks/{id}/status with ownership violation returns 403 Forbidden")
    void updateStatusCrossUserForbiddenReturns403() throws Exception {
        UUID id = UUID.randomUUID();
        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest(TaskStatus.IN_PROGRESS);
        when(taskService.updateTaskStatus(eq(id), any(TaskStatusUpdateRequest.class)))
            .thenThrow(new TaskOwnershipException("You are not authorized to access or modify this task"));

        mockMvc.perform(patch("/api/tasks/{id}/status", id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}
