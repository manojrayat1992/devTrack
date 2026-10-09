package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.time.LocalDate;

public record TaskFilterCriteria(
    TaskStatus status,
    TaskPriority priority,
    LocalDate dueBefore,
    LocalDate dueAfter,
    String search
) {}
