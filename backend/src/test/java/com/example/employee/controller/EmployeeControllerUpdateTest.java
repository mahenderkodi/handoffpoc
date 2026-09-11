package com.example.employee.controller;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.dto.EmployeeUpdateRequest;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.DuplicateEmployeeException;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerUpdateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private EmployeeUpdateRequest validRequest() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Asha");
        request.setLastName("Iyer");
        request.setEmail("asha.iyer@example.com");
        request.setPhone("+91-90000-00000");
        request.setDepartment("Platform");
        request.setJobTitle("Senior Backend Developer");
        request.setDateOfJoining(LocalDate.of(2024, 3, 1));
        request.setStatus(EmployeeStatus.ACTIVE);
        return request;
    }

    @Test
    void update_validPayload_returns200WithUpdatedEmployee() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 12, 9, 0, 0);
        EmployeeResponse response = new EmployeeResponse(17L, "EMP-1042", "Asha", "Iyer",
                "asha.iyer@example.com", "+91-90000-00000", "Platform", "Senior Backend Developer",
                LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE, now, now);

        when(employeeService.updateEmployee(eq(17L), any())).thenReturn(response);

        mockMvc.perform(put("/api/employees/17")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.employeeCode").value("EMP-1042"))
                .andExpect(jsonPath("$.lastName").value("Iyer"))
                .andExpect(jsonPath("$.email").value("asha.iyer@example.com"));
    }

    @Test
    void update_missingRequiredFields_returns400WithValidationErrorShape() throws Exception {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest(); // everything blank/null

        mockMvc.perform(put("/api/employees/17")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
    }

    @Test
    void update_notFound_returns404WithErrorShape() throws Exception {
        when(employeeService.updateEmployee(eq(99L), any()))
                .thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(put("/api/employees/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/employees/99"));
    }

    @Test
    void update_duplicateEmail_returns409WithDuplicateResourceShape() throws Exception {
        when(employeeService.updateEmployee(eq(17L), any()))
                .thenThrow(new DuplicateEmployeeException("email", "asha.iyer@example.com"));

        mockMvc.perform(put("/api/employees/17")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }
}
