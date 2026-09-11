package com.example.employee.repository;

import com.example.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * No generic base repository (Section 5.3) — {@link JpaRepository} already provides everything
 * this POC needs. Additional derived-query methods (search, filter by department/status) will
 * be added when the list/search functionality is implemented.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);
}
