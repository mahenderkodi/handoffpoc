package com.example.employee.service;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceGetByIdTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository);
    }

    @Test
    void getEmployee_found_returnsResponse() {
        Employee e = new Employee("EMP-1042", "Asha", "Rao", "asha.rao@example.com", "+91-98765-43210",
                "Engineering", "Backend Developer", LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE);
        setField(e, "id", 17L);
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 10, 15, 30);
        setField(e, "createdAt", now);
        setField(e, "updatedAt", now);

        when(employeeRepository.findById(17L)).thenReturn(Optional.of(e));

        EmployeeResponse response = employeeService.getEmployee(17L);

        assertThat(response.getId()).isEqualTo(17L);
        assertThat(response.getEmployeeCode()).isEqualTo("EMP-1042");
        assertThat(response.getEmail()).isEqualTo("asha.rao@example.com");
    }

    @Test
    void getEmployee_notFound_throws() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = Employee.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
