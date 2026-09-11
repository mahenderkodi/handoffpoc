package com.example.employee.dto;

import com.example.employee.entity.EmployeeStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What every successful employee endpoint returns. Never the entity itself — Section 5.1
 * requires DTOs at the REST boundary.
 */
public class EmployeeResponse {

    private final Long id;
    private final String employeeCode;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String department;
    private final String jobTitle;
    private final LocalDate dateOfJoining;
    private final EmployeeStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public EmployeeResponse(Long id, String employeeCode, String firstName, String lastName, String email,
                             String phone, String department, String jobTitle, LocalDate dateOfJoining,
                             EmployeeStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.employeeCode = employeeCode;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.department = department;
        this.jobTitle = jobTitle;
        this.dateOfJoining = dateOfJoining;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeCode() {
        return employeeCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDepartment() {
        return department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
