package com.devtrack.exception;

import com.devtrack.entity.TaskStatus;
import java.util.List;

public class InvalidStatusTransitionException extends DevTrackException {

    private final TaskStatus currentStatus;
    private final TaskStatus attemptedStatus;
    private final List<TaskStatus> allowedStatuses;

    public InvalidStatusTransitionException(String message) {
        super(message);
        this.currentStatus = null;
        this.attemptedStatus = null;
        this.allowedStatuses = List.of();
    }

    public InvalidStatusTransitionException(TaskStatus currentStatus, TaskStatus attemptedStatus, List<TaskStatus> allowedStatuses) {
        super(String.format("Invalid status transition from '%s' to '%s'. Allowed transitions: %s",
                currentStatus, attemptedStatus, allowedStatuses));
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.allowedStatuses = allowedStatuses;
    }

    public TaskStatus getCurrentStatus() {
        return currentStatus;
    }

    public TaskStatus getAttemptedStatus() {
        return attemptedStatus;
    }

    public List<TaskStatus> getAllowedStatuses() {
        return allowedStatuses;
    }
}
