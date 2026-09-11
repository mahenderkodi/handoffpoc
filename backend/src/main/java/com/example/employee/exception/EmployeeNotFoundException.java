package com.example.employee.exception;

/**
 * Reserved for when GET-by-id / update / delete are implemented (not in this first slice —
 * Create is the only functionality built so far). Declared now, per Section 5.1's package
 * layout, so later slices don't need to touch the exception package's shape.
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(Long id) {
        super("Employee not found: " + id);
    }
}
