package com.example.employee.exception;

/**
 * Thrown by the service layer when a create (or, later, update) would violate the uniqueness
 * of {@code employeeCode} or {@code email}. Carries the offending field so
 * {@link GlobalExceptionHandler} can report it via Section 9's {@code errors[]} array.
 */
public class DuplicateEmployeeException extends RuntimeException {

    private final String field;
    private final String value;

    public DuplicateEmployeeException(String field, String value) {
        super("An employee with %s '%s' already exists".formatted(field, value));
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
