package com.example.employee.mapper;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.EmployeeUpdateRequest;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;

/**
 * Hand-written entity/DTO mapping — deliberately not a library or code generator (Section 5.1):
 * there is exactly one entity in this POC, so a mapping library would be pure overhead.
 */
public final class EmployeeMapper {

    private EmployeeMapper() {
    }

    public static Employee toEntity(EmployeeCreateRequest request) {
        EmployeeStatus status = request.getStatus() != null ? request.getStatus() : EmployeeStatus.ACTIVE;
        return new Employee(
                request.getEmployeeCode(),
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPhone(),
                request.getDepartment(),
                request.getJobTitle(),
                request.getDateOfJoining(),
                status
        );
    }

    /** Mutates {@code employee} in place with every field PUT allows a client to change.
     * employeeCode, id, and createdAt are never touched here — Section 5.2. */
    public static void applyUpdate(Employee employee, EmployeeUpdateRequest request) {
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setJobTitle(request.getJobTitle());
        employee.setDateOfJoining(request.getDateOfJoining());
        employee.setStatus(request.getStatus());
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getDepartment(),
                employee.getJobTitle(),
                employee.getDateOfJoining(),
                employee.getStatus(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}
