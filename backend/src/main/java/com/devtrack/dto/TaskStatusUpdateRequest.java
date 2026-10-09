package com.devtrack.dto;

import com.devtrack.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusUpdateRequest(
    @NotNull(message = "Task status is required")
    TaskStatus status
) {}
