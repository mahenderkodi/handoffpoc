package com.example.employee.dto;

import com.example.employee.entity.EmployeeStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Request body for PUT /api/employees/{id}. Deliberately has no {@code employeeCode} field —
 * Section 5.2 requires employeeCode to stay immutable after creation, so rather than accepting
 * it and silently ignoring it, this type simply cannot carry it; any {@code employeeCode} sent
 * by a client is ignored by Jackson as an unrecognized property.
 */
public class EmployeeUpdateRequest {

    @NotBlank(message = "firstName is required")
    @Size(max = 100, message = "firstName must be at most 100 characters")
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(max = 100, message = "lastName must be at most 100 characters")
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 150, message = "email must be at most 150 characters")
    private String email;

    @Pattern(regexp = "^\\+?[0-9 -]{7,20}$", message = "phone must be 7-20 characters: digits, spaces, dashes, optional leading +")
    private String phone;

    @NotBlank(message = "department is required")
    @Size(max = 100, message = "department must be at most 100 characters")
    private String department;

    @NotBlank(message = "jobTitle is required")
    @Size(max = 100, message = "jobTitle must be at most 100 characters")
    private String jobTitle;

    @NotNull(message = "dateOfJoining is required")
    @PastOrPresent(message = "dateOfJoining must not be in the future")
    private LocalDate dateOfJoining;

    @NotNull(message = "status is required")
    private EmployeeStatus status;

    public EmployeeUpdateRequest() {
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public LocalDate getDateOfJoining() {
        return dateOfJoining;
    }

    public void setDateOfJoining(LocalDate dateOfJoining) {
        this.dateOfJoining = dateOfJoining;
    }

    public EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(EmployeeStatus status) {
        this.status = status;
    }
}
