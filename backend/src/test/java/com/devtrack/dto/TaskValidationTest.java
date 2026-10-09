package com.devtrack.dto;

import com.devtrack.entity.TaskPriority;
import com.devtrack.entity.TaskStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Bean Validation Tests")
class TaskValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    @DisplayName("Valid TaskCreateRequest passes validation")
    void validTaskCreateRequest() {
        TaskCreateRequest request = new TaskCreateRequest(
            "Implement OAuth2 Resource Server",
            "Configure Spring Security with Keycloak JWT validation",
            TaskPriority.HIGH,
            TaskStatus.OPEN,
            LocalDate.now().plusDays(3)
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Expected no validation violations");
    }

    @Test
    @DisplayName("Blank title fails validation")
    void blankTitleFailsValidation() {
        TaskCreateRequest request = new TaskCreateRequest(
            "   ",
            "Description",
            TaskPriority.MEDIUM,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("Title shorter than 3 characters fails validation")
    void shortTitleFailsValidation() {
        TaskCreateRequest request = new TaskCreateRequest(
            "AB",
            "Description",
            TaskPriority.MEDIUM,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("Null priority fails validation")
    void nullPriorityFailsValidation() {
        TaskCreateRequest request = new TaskCreateRequest(
            "Valid Title",
            "Description",
            null,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("priority")));
    }

    @Test
    @DisplayName("Description exceeding 4000 characters fails validation")
    void excessiveDescriptionFailsValidation() {
        String longDescription = "A".repeat(4001);
        TaskCreateRequest request = new TaskCreateRequest(
            "Valid Title",
            longDescription,
            TaskPriority.LOW,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("description")));
    }
}
