package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.mapper.EmployeeMapper;
import com.example.employee.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public EmployeeResponse createEmployee(EmployeeCreateRequest request) {
        // Business rules (Section 5.2): reject duplicate email / employeeCode before insert.
        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmployeeException("email", request.getEmail());
        }
        if (employeeRepository.existsByEmployeeCode(request.getEmployeeCode())) {
            throw new DuplicateEmployeeException("employeeCode", request.getEmployeeCode());
        }

        Employee employee = EmployeeMapper.toEntity(request);
        // createdAt/updatedAt are set by Employee's @PrePersist — never accepted from the client.
        Employee saved = employeeRepository.save(employee);

        log.info("Created employee id={}", saved.getId());

        return EmployeeMapper.toResponse(saved);
    }
}
