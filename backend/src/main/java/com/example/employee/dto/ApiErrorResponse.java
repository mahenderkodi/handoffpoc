package com.example.employee.dto;

import java.time.Instant;
import java.util.List;

/**
 * The exact error shape every backend error response must follow — Section 9 of
 * IMPLEMENTATION_INSTRUCTIONS.md. Built only by {@link com.example.employee.exception.GlobalExceptionHandler}.
 */
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final List<FieldErrorDto> errors;

    public ApiErrorResponse(Instant timestamp, int status, String error, String message, String path,
                             List<FieldErrorDto> errors) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.errors = errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public List<FieldErrorDto> getErrors() {
        return errors;
    }
}
