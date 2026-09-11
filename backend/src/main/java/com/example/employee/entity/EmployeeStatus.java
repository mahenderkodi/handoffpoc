package com.example.employee.entity;

/**
 * Employment status. Stored as a VARCHAR with a database-level CHECK constraint
 * (see db/schema.sql) rather than MySQL's native ENUM type — deliberate choice,
 * see IMPLEMENTATION_INSTRUCTIONS.md Section 6.
 */
public enum EmployeeStatus {
    ACTIVE,
    INACTIVE
}
