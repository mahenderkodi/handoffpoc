package com.example.employee.repository;

import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository for Employee entities. A custom search method is provided to support
 * the list/search API with optional free-text query and optional status filter.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("SELECT e FROM Employee e WHERE (:q IS NULL OR lower(e.firstName) LIKE lower(concat('%', :q, '%')) "
            + "OR lower(e.lastName) LIKE lower(concat('%', :q, '%')) OR lower(e.email) LIKE lower(concat('%', :q, '%'))) "
            + "AND (:status IS NULL OR e.status = :status)")
    Page<Employee> search(@Param("q") String q, @Param("status") EmployeeStatus status, Pageable pageable);
}
