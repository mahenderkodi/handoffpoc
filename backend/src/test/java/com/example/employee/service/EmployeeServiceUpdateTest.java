package com.example.employee.service;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.EmployeeUpdateRequest;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Section 11: covers not-found handling, duplicate-email rejection (excluding self), and
 * employeeCode immutability on update — the cases EmployeeServiceImplTest's javadoc flagged
 * as out of scope until Update existed.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceUpdateTest {

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

    private EmployeeUpdateRequest updateRequest() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Asha");
        request.setLastName("Iyer"); // changed surname
        request.setEmail("asha.iyer@example.com"); // changed email
        request.setPhone("+91-90000-00000");
        request.setDepartment("Platform");
        request.setJobTitle("Senior Backend Developer");
        request.setDateOfJoining(LocalDate.of(2024, 3, 1));
        request.setStatus(EmployeeStatus.ACTIVE);
        return request;
    }

    @Test
    void updateEmployee_success_updatesFieldsAndKeepsEmployeeCode() {
        Employee employee = existingEmployee();
        when(employeeRepository.findById(17L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("asha.iyer@example.com", 17L)).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        EmployeeResponse response = employeeService.updateEmployee(17L, updateRequest());

        assertThat(response.getEmployeeCode()).isEqualTo("EMP-1042"); // unchanged
        assertThat(response.getLastName()).isEqualTo("Iyer");
        assertThat(response.getEmail()).isEqualTo("asha.iyer@example.com");
        assertThat(response.getDepartment()).isEqualTo("Platform");
        verify(employeeRepository).save(employee);
    }

    @Test
    void updateEmployee_notFound_throwsAndNeverSaves() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.updateEmployee(99L, updateRequest()))
                .isInstanceOf(EmployeeNotFoundException.class)
                .hasMessageContaining("99");

        verify(employeeRepository, never()).save(any());
    }

    @Test
    void updateEmployee_emailBelongsToDifferentEmployee_throwsAndNeverSaves() {
        Employee employee = existingEmployee();
        when(employeeRepository.findById(17L)).thenReturn(Optional.of(employee));
        when(employeeRepository.existsByEmailAndIdNot("asha.iyer@example.com", 17L)).thenReturn(true);

        assertThatThrownBy(() -> employeeService.updateEmployee(17L, updateRequest()))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining("email");

        verify(employeeRepository, never()).save(any());
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
