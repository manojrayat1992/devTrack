package com.devtrack.service;

import com.devtrack.entity.TaskStatus;
import com.devtrack.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TaskStatusTransitionEngine Unit Tests")
class TaskStatusTransitionEngineTest {

    private TaskStatusTransitionEngine transitionEngine;

    @BeforeEach
    void setUp() {
        transitionEngine = new TaskStatusTransitionEngine();
    }

    @Test
    @DisplayName("OPEN status allows transitions to IN_PROGRESS and COMPLETED")
    void openStatusAllowedTransitions() {
        List<TaskStatus> allowed = transitionEngine.getAllowedNextStatuses(TaskStatus.OPEN);
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(TaskStatus.IN_PROGRESS));
        assertTrue(allowed.contains(TaskStatus.COMPLETED));
    }

    @Test
    @DisplayName("IN_PROGRESS status allows transitions to IN_REVIEW, OPEN, and COMPLETED")
    void inProgressStatusAllowedTransitions() {
        List<TaskStatus> allowed = transitionEngine.getAllowedNextStatuses(TaskStatus.IN_PROGRESS);
        assertEquals(3, allowed.size());
        assertTrue(allowed.contains(TaskStatus.IN_REVIEW));
        assertTrue(allowed.contains(TaskStatus.OPEN));
        assertTrue(allowed.contains(TaskStatus.COMPLETED));
    }

    @Test
    @DisplayName("IN_REVIEW status allows transitions to COMPLETED and IN_PROGRESS")
    void inReviewStatusAllowedTransitions() {
        List<TaskStatus> allowed = transitionEngine.getAllowedNextStatuses(TaskStatus.IN_REVIEW);
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(TaskStatus.COMPLETED));
        assertTrue(allowed.contains(TaskStatus.IN_PROGRESS));
    }

    @Test
    @DisplayName("COMPLETED status allows reopening to OPEN or IN_PROGRESS")
    void completedStatusAllowedTransitions() {
        List<TaskStatus> allowed = transitionEngine.getAllowedNextStatuses(TaskStatus.COMPLETED);
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(TaskStatus.OPEN));
        assertTrue(allowed.contains(TaskStatus.IN_PROGRESS));
    }

    @ParameterizedTest
    @CsvSource({
        "OPEN, IN_REVIEW",
        "IN_REVIEW, OPEN"
    })
    @DisplayName("Invalid transitions throw InvalidStatusTransitionException")
    void invalidTransitionsThrowException(TaskStatus from, TaskStatus to) {
        InvalidStatusTransitionException exception = assertThrows(
            InvalidStatusTransitionException.class,
            () -> transitionEngine.validateTransition(from, to)
        );

        assertEquals(from, exception.getCurrentStatus());
        assertEquals(to, exception.getAttemptedStatus());
        assertFalse(exception.getAllowedStatuses().contains(to));
    }

    @Test
    @DisplayName("Transition to identical status is valid (idempotent)")
    void sameStatusTransitionIsPermitted() {
        assertDoesNotThrow(() -> transitionEngine.validateTransition(TaskStatus.OPEN, TaskStatus.OPEN));
        assertDoesNotThrow(() -> transitionEngine.validateTransition(TaskStatus.IN_PROGRESS, TaskStatus.IN_PROGRESS));
    }
}
