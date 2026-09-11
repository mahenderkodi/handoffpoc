package com.example.employee.service;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceListTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository);
    }

    @Test
    void listEmployees_returnsPagedResponses() {
        Employee e = new Employee("EMP-1", "Asha", "Rao", "asha.rao@example.com", "+91-98765-43210",
                "Engineering", "Backend Developer", LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE);
        // set id/timestamps via reflection similar to other tests
        try {
            var idField = Employee.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(e, 17L);
            var created = LocalDateTime.now();
            var createdField = Employee.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(e, created);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        PageImpl<Employee> page = new PageImpl<>(List.of(e), PageRequest.of(0, 10), 1);

        when(employeeRepository.search(any(), any(), any())).thenReturn(page);

        Page<EmployeeResponse> result = employeeService.listEmployees("Asha", null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        EmployeeResponse resp = result.getContent().get(0);
        assertThat(resp.getId()).isEqualTo(17L);
        assertThat(resp.getEmail()).isEqualTo("asha.rao@example.com");
    }
}
