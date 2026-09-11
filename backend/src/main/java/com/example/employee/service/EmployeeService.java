package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;

/**
 * The one layer that gets an interface (Section 5.3) — it's where a test seam and
 * conventional DI genuinely help; the mapper and exceptions do not need one.
 */
public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);
}
