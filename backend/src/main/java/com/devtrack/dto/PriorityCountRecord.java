package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;

public record PriorityCountRecord(
    TaskPriority priority,
    long count
) {}
