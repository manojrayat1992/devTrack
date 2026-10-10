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

    @Test
    @DisplayName("Whitespace-padded short title is stripped and fails validation")
    void whitespacePaddedShortTitleFailsValidation() {
        TaskCreateRequest request = new TaskCreateRequest(
            "  ab  ",
            "Valid description",
            TaskPriority.MEDIUM,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("title")));
    }

    @Test
    @DisplayName("Whitespace-padded valid title is stripped and passes validation")
    void whitespacePaddedValidTitlePassesValidation() {
        TaskCreateRequest request = new TaskCreateRequest(
            "  abc  ",
            "Valid description",
            TaskPriority.MEDIUM,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        assertEquals("abc", request.title());
        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Description with exactly 4000 characters passes validation")
    void maxAllowedDescriptionPassesValidation() {
        String maxDescription = "A".repeat(4000);
        TaskCreateRequest request = new TaskCreateRequest(
            "Valid Title",
            maxDescription,
            TaskPriority.LOW,
            TaskStatus.OPEN,
            LocalDate.now()
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Null description passes validation")
    void nullDescriptionPassesValidation() {
        TaskCreateRequest request = new TaskCreateRequest(
            "Valid Title",
            null,
            TaskPriority.LOW,
            TaskStatus.OPEN,
            null
        );

        Set<ConstraintViolation<TaskCreateRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("TaskUpdateRequest validates correctly on boundary values")
    void taskUpdateRequestValidation() {
        TaskUpdateRequest validRequest = new TaskUpdateRequest(
            "Updated Title",
            "Updated description",
            TaskPriority.CRITICAL,
            null
        );
        assertTrue(validator.validate(validRequest).isEmpty());

        TaskUpdateRequest invalidRequest = new TaskUpdateRequest(
            "  x  ",
            null,
            null,
            null
        );
        Set<ConstraintViolation<TaskUpdateRequest>> violations = validator.validate(invalidRequest);
        assertEquals(2, violations.size()); // title < 3 and priority is null
    }
}
