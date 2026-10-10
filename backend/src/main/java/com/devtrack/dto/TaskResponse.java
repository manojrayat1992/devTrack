package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    LocalDate dueDate,
    String ownerId,
    Instant createdAt,
    Instant updatedAt,
    List<TaskStatus> allowedNextStatuses,
    Long version
) {
    public TaskResponse(
            UUID id,
            String title,
            String description,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDate,
            String ownerId,
            Instant createdAt,
            Instant updatedAt,
            List<TaskStatus> allowedNextStatuses) {
        this(id, title, description, priority, status, dueDate, ownerId, createdAt, updatedAt, allowedNextStatuses, 0L);
    }
}
