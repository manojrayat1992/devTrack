package com.devtrack.exception;

import com.devtrack.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            "The requested resource was not found: " + ex.getResourcePath(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException ex, HttpServletRequest request) {
        log.warn("Invalid status transition: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Status Transition",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(TaskOwnershipException.class)
    public ResponseEntity<ErrorResponse> handleTaskOwnership(TaskOwnershipException ex, HttpServletRequest request) {
        log.warn("Task ownership violation: {}", ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on path {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "Forbidden",
            "You do not have authorization to access this resource",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic locking conflict on path {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "Conflict",
            "The task was modified by another concurrent transaction. Please reload the task and retry your change.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handlePropertyReference(PropertyReferenceException ex, HttpServletRequest request) {
        log.warn("Invalid property reference on path {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Invalid sorting or filter property: '" + ex.getPropertyName() + "'",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Method argument type mismatch on path {}: {}", request.getRequestURI(), ex.getMessage());
        Class<?> requiredType = ex.getRequiredType();
        String message;
        if (requiredType != null && requiredType.isEnum()) {
            Object[] enumConstants = requiredType.getEnumConstants();
            message = String.format("Parameter '%s' has an invalid value '%s'. Expected one of: %s",
                ex.getName(), ex.getValue(), java.util.Arrays.toString(enumConstants));
        } else {
            message = String.format("Parameter '%s' has an invalid value format: '%s'", ex.getName(), ex.getValue());
        }
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            message,
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(org.springframework.web.bind.MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing request parameter on path {}: {}", request.getRequestURI(), ex.getParameterName());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Required request parameter '" + ex.getParameterName() + "' of type '" + ex.getParameterType() + "' is missing",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(org.springframework.web.bind.MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Missing request header on path {}: {}", request.getRequestURI(), ex.getHeaderName());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Required request header '" + ex.getHeaderName() + "' is missing",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(org.springframework.web.HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Unsupported Media Type on path {}: {}", request.getRequestURI(), ex.getContentType());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            "Unsupported Media Type",
            "Content-Type '" + ex.getContentType() + "' is not supported. Supported media types: " + ex.getSupportedMediaTypes(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(response);
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(org.springframework.web.HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        log.warn("Media Type Not Acceptable on path {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.NOT_ACCEPTABLE.value(),
            "Not Acceptable",
            "Requested media type is not acceptable. Acceptable types: " + ex.getSupportedMediaTypes(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HTTP method not supported on path {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            "Method Not Allowed",
            "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Validation failed for {}: {}", request.getRequestURI(), fieldErrors);
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Input validation failed for one or more fields",
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        log.warn("Constraint violation on {}: {}", request.getRequestURI(), fieldErrors);
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Validation Failed",
            "Constraint violation on input parameters",
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed HTTP request body on {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            "Malformed request body or invalid value format",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument on {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal error processing request {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal Server Error",
            "An unexpected error occurred. Please contact the administrator.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
