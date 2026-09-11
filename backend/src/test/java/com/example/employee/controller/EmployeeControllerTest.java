package com.example.employee.controller;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.DuplicateEmployeeException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Section 11: "Test the controller layer in isolation (mock the service): cover a valid create
 * returning 201, an invalid payload returning 400 with the expected error shape..." (a missing
 * employee returning 404 is out of scope here — no GET endpoint exists in this slice yet).
 */
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void create_validPayload_returns201WithLocationHeader() throws Exception {
        EmployeeCreateRequest request = validRequest();
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 10, 15, 30);
        EmployeeResponse response = new EmployeeResponse(17L, "EMP-1042", "Asha", "Rao",
                "asha.rao@example.com", "+91-98765-43210", "Engineering", "Backend Developer",
                LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE, now, now);

        when(employeeService.createEmployee(any())).thenReturn(response);

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsPath("/api/employees/17")))
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.employeeCode").value("EMP-1042"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void create_missingRequiredFields_returns400WithValidationErrorShape() throws Exception {
        EmployeeCreateRequest request = new EmployeeCreateRequest(); // everything blank/null

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/employees"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists());
    }

    @Test
    void create_duplicateEmail_returns409WithDuplicateResourceShape() throws Exception {
        EmployeeCreateRequest request = validRequest();
        when(employeeService.createEmployee(any()))
                .thenThrow(new DuplicateEmployeeException("email", request.getEmail()));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_RESOURCE"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
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
        request.setStatus(EmployeeStatus.ACTIVE);
        return request;
    }

    private static org.hamcrest.Matcher<String> containsPath(String path) {
        return org.hamcrest.Matchers.containsString(path);
    }
}
