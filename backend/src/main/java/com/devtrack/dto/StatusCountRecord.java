package com.devtrack.dto;

import com.devtrack.entity.TaskStatus;

public record StatusCountRecord(
    TaskStatus status,
    long count
) {}
