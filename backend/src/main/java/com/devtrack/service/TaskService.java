package com.devtrack.service;

import com.devtrack.dto.PageResponse;
import com.devtrack.dto.TaskCreateRequest;
import com.devtrack.dto.TaskFilterCriteria;
import com.devtrack.dto.TaskResponse;
import com.devtrack.dto.TaskStatusUpdateRequest;
import com.devtrack.dto.TaskUpdateRequest;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    TaskResponse createTask(TaskCreateRequest request);

    TaskResponse getTaskById(UUID id);

    TaskResponse updateTask(UUID id, TaskUpdateRequest request);

    TaskResponse updateTaskStatus(UUID id, TaskStatusUpdateRequest request);

    void deleteTask(UUID id);

    PageResponse<TaskResponse> findTasks(TaskFilterCriteria criteria, Pageable pageable);
}
