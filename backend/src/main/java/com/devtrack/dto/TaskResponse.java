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
    List<TaskStatus> allowedNextStatuses
) {}
