package com.example.employee.exception;

/**
 * Thrown by GET-by-id when no employee matches the given id; update and delete will
 * reuse it once those endpoints exist.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found: " + id);
    }
}
