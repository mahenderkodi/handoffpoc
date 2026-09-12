package com.example.employee.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Section 11: covers successful deletion and not-found handling for delete
 * (Section 7), following the same pattern as EmployeeServiceUpdateTest.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceDeleteTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository);
    }

    private Employee existingEmployee() {
        Employee employee = new Employee("EMP-1042", "Asha", "Rao", "asha.rao@example.com",
                "+91-98765-43210", "Engineering", "Backend Developer",
                LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE);
        setField(employee, "id", 17L);
        return employee;
    }

    @Test
    void deleteEmployee_success_deletesViaRepository() {
        Employee employee = existingEmployee();
        when(employeeRepository.findById(17L)).thenReturn(Optional.of(employee));

        employeeService.deleteEmployee(17L);

        verify(employeeRepository).delete(employee);
    }

    @Test
    void deleteEmployee_notFound_throwsAndNeverDeletes() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.deleteEmployee(99L))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");

        verify(employeeRepository, never()).delete(any());
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
