package com.example.employee.dto;

/** One entry in {@link ApiErrorResponse#getErrors()} — Section 9's error contract. */
public class FieldErrorDto {

    private final String field;
    private final String message;

    public FieldErrorDto(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}
