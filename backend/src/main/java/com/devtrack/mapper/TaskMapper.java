package com.devtrack.mapper;

import com.devtrack.dto.TaskCreateRequest;
import com.devtrack.dto.TaskResponse;
import com.devtrack.dto.TaskUpdateRequest;
import com.devtrack.entity.Task;
import com.devtrack.entity.TaskStatus;
import com.devtrack.exception.InvalidStatusTransitionException;
import com.devtrack.service.TaskStatusTransitionEngine;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TaskMapper {

    private final TaskStatusTransitionEngine transitionEngine;

    public TaskMapper(TaskStatusTransitionEngine transitionEngine) {
        this.transitionEngine = transitionEngine;
    }

    public Task toEntity(TaskCreateRequest request, String ownerId) {
        Task task = new Task();
        task.setTitle(request.title().trim());
        task.setDescription(request.description() != null ? request.description().trim() : null);
        task.setPriority(request.priority());
        // Business rule: Newly created tasks must always begin in the OPEN lifecycle state
        if (request.status() != null && request.status() != TaskStatus.OPEN) {
            throw new InvalidStatusTransitionException(
                "Initial task state must be OPEN; cannot directly create task in status '" + request.status() + "'"
            );
        }
        task.setStatus(TaskStatus.OPEN);
        task.setDueDate(request.dueDate());
        task.setOwnerId(ownerId);
        return task;
    }

    public TaskResponse toResponse(Task task) {
        List<TaskStatus> allowedNext = transitionEngine.getAllowedNextStatuses(task.getStatus());
        return new TaskResponse(
            task.getId(),
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getStatus(),
            task.getDueDate(),
            task.getOwnerId(),
            task.getCreatedAt(),
            task.getUpdatedAt(),
            allowedNext,
            task.getVersion() != null ? task.getVersion() : 0L
        );
    }

    public void updateEntity(Task task, TaskUpdateRequest request) {
        task.setTitle(request.title().trim());
        task.setDescription(request.description() != null ? request.description().trim() : null);
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
    }
}
