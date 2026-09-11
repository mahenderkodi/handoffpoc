package com.example.employee.exception;

import com.example.employee.dto.ApiErrorResponse;
import com.example.employee.dto.FieldErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Single global exception-handling component (Section 9 / Section 5.1) — no individual
 * controller method needs its own try/catch. Every response body follows the exact shape
 * documented in IMPLEMENTATION_INSTRUCTIONS.md Section 9.
 *
 * <p>{@link com.example.employee.exception.EmployeeNotFoundException} handling covers GET by id;
 * update and delete will raise it too once those endpoints exist.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest request) {
        List<FieldErrorDto> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorDto)
                .toList();

        log.warn("Validation failed on {} ({} field error(s))", request.getRequestURI(), errors.size());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Validation failed", request.getRequestURI(), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicateEmployeeException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateEmployeeException ex,
                                                              HttpServletRequest request) {
        log.warn("Duplicate employee on {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), HttpStatus.CONFLICT.value(), "DUPLICATE_RESOURCE",
                ex.getMessage(), request.getRequestURI(),
                List.of(new FieldErrorDto(ex.getField(), "already in use")));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(EmployeeNotFoundException ex,
                                                              HttpServletRequest request) {
        log.warn("Employee not found on {}: {}", request.getRequestURI(), ex.getMessage());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), HttpStatus.NOT_FOUND.value(), "NOT_FOUND",
                ex.getMessage(), request.getRequestURI(), List.of());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                                  HttpServletRequest request) {
        // Backstop for a unique-constraint violation that slips past the service-layer check
        // (e.g. a race between two concurrent creates) — Section 6.
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), HttpStatus.CONFLICT.value(), "DATA_INTEGRITY_VIOLATION",
                "The request conflicts with existing data", request.getRequestURI(), List.of());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Never leak the underlying exception message or stack trace to the client (Section 9) —
        // log it server-side instead.
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);

        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "An unexpected error occurred", request.getRequestURI(), List.of());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private FieldErrorDto toFieldErrorDto(FieldError fieldError) {
        return new FieldErrorDto(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
