package com.devtrack.event;

import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Java 21 Sealed Domain Event hierarchy for task lifecycle events.
 * Enforces exhaustive pattern matching when consumed by audit listeners and event processors.
 */
public sealed interface TaskDomainEvent permits
    TaskDomainEvent.TaskCreatedEvent,
    TaskDomainEvent.TaskStatusChangedEvent,
    TaskDomainEvent.TaskDeletedEvent {

    UUID taskId();
    String ownerId();
    Instant timestamp();

    record TaskCreatedEvent(
        UUID taskId,
        String ownerId,
        String title,
        TaskPriority priority,
        Instant timestamp
    ) implements TaskDomainEvent {}

    record TaskStatusChangedEvent(
        UUID taskId,
        String ownerId,
        TaskStatus fromStatus,
        TaskStatus toStatus,
        Instant timestamp
    ) implements TaskDomainEvent {}

    record TaskDeletedEvent(
        UUID taskId,
        String ownerId,
        Instant timestamp
    ) implements TaskDomainEvent {}
}
