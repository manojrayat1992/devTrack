package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record TaskUpdateRequest(
    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 255, message = "Task title must be between 3 and 255 characters")
    String title,

    @Size(max = 4000, message = "Description must not exceed 4000 characters")
    String description,

    @NotNull(message = "Task priority is required")
    TaskPriority priority,

    LocalDate dueDate
) {}
