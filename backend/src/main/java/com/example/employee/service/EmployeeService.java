package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.EmployeeUpdateRequest;
import com.example.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * The service interface for employee operations. Includes listing with search and
 * pagination in addition to creation.
 */
public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    Page<EmployeeResponse> listEmployees(String q, EmployeeStatus status, Pageable pageable);

    EmployeeResponse getEmployee(Long id);

    EmployeeResponse updateEmployee(Long id, EmployeeUpdateRequest request);

    void deleteEmployee(Long id);
}
