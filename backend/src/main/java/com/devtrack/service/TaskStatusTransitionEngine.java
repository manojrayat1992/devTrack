package com.devtrack.service;

import com.devtrack.entity.TaskStatus;
import com.devtrack.exception.InvalidStatusTransitionException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TaskStatusTransitionEngine {

    private static final Map<TaskStatus, List<TaskStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TaskStatus.OPEN, List.of(
            TaskStatus.IN_PROGRESS,
            TaskStatus.COMPLETED
        ));

        ALLOWED_TRANSITIONS.put(TaskStatus.IN_PROGRESS, List.of(
            TaskStatus.IN_REVIEW,
            TaskStatus.OPEN,
            TaskStatus.COMPLETED
        ));

        ALLOWED_TRANSITIONS.put(TaskStatus.IN_REVIEW, List.of(
            TaskStatus.COMPLETED,
            TaskStatus.IN_PROGRESS
        ));

        ALLOWED_TRANSITIONS.put(TaskStatus.COMPLETED, List.of(
            TaskStatus.OPEN,
            TaskStatus.IN_PROGRESS
        ));
    }

    public List<TaskStatus> getAllowedNextStatuses(TaskStatus currentStatus) {
        if (currentStatus == null) {
            return Collections.emptyList();
        }
        return ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptyList());
    }

    public boolean canTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        if (currentStatus == newStatus) {
            return true; // No-op transition is permissible
        }
        return getAllowedNextStatuses(currentStatus).contains(newStatus);
    }

    public void validateTransition(TaskStatus currentStatus, TaskStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }
        List<TaskStatus> allowed = getAllowedNextStatuses(currentStatus);
        if (!allowed.contains(targetStatus)) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus, allowed);
        }
    }
}
