package com.example.employee.service;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.Employee;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Section 11: "Unit test the service layer with the repository mocked: cover duplicate-email
 * rejection, duplicate-code rejection..." (not-found handling is covered in
 * {@link EmployeeServiceGetByIdTest}; employeeCode immutability on update is covered in
 * {@link EmployeeServiceUpdateTest}).
 */
@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeServiceImpl(employeeRepository);
    }

    private EmployeeCreateRequest validRequest() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmployeeCode("EMP-1042");
        request.setFirstName("Asha");
        request.setLastName("Rao");
        request.setEmail("asha.rao@example.com");
        request.setPhone("+91-98765-43210");
        request.setDepartment("Engineering");
        request.setJobTitle("Backend Developer");
        request.setDateOfJoining(LocalDate.of(2024, 3, 1));
        request.setStatus(null); // let the service default it
        return request;
    }

    @Test
    void createEmployee_savesAndReturnsResponse_defaultingStatusToActive() {
        EmployeeCreateRequest request = validRequest();
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode(request.getEmployeeCode())).thenReturn(false);

        Employee saved = new Employee(request.getEmployeeCode(), request.getFirstName(), request.getLastName(),
                request.getEmail(), request.getPhone(), request.getDepartment(), request.getJobTitle(),
                request.getDateOfJoining(), EmployeeStatus.ACTIVE);
        setId(saved, 17L);
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 10, 15, 30);
        setTimestamps(saved, now, now);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeResponse response = employeeService.createEmployee(request);

        assertThat(response.getId()).isEqualTo(17L);
        assertThat(response.getEmployeeCode()).isEqualTo("EMP-1042");
        assertThat(response.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(response.getCreatedAt()).isEqualTo(now);
        assertThat(response.getUpdatedAt()).isEqualTo(now);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void createEmployee_duplicateEmail_throwsAndNeverSaves() {
        EmployeeCreateRequest request = validRequest();
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining("email");

        verify(employeeRepository, never()).save(any());
        verify(employeeRepository, never()).existsByEmployeeCode(any());
    }

    @Test
    void createEmployee_duplicateEmployeeCode_throwsAndNeverSaves() {
        EmployeeCreateRequest request = validRequest();
        when(employeeRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(employeeRepository.existsByEmployeeCode(request.getEmployeeCode())).thenReturn(true);

        assertThatThrownBy(() -> employeeService.createEmployee(request))
                .isInstanceOf(DuplicateEmployeeException.class)
                .hasMessageContaining("employeeCode");

        verify(employeeRepository, never()).save(any());
    }

    // --- test-only reflection helpers, since Employee has no setters for id/timestamps ---

    private void setId(Employee employee, Long id) {
        setField(employee, "id", id);
    }

    private void setTimestamps(Employee employee, LocalDateTime createdAt, LocalDateTime updatedAt) {
        setField(employee, "createdAt", createdAt);
        setField(employee, "updatedAt", updatedAt);
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
