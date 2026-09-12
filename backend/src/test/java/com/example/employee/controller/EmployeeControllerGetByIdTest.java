package com.example.employee.controller;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.exception.EmployeeNotFoundException;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerGetByIdTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void getById_found_returns200WithEmployeeJson() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 11, 10, 15, 30);
        EmployeeResponse response = new EmployeeResponse(17L, "EMP-1042", "Asha", "Rao",
                "asha.rao@example.com", "+91-98765-43210", "Engineering", "Backend Developer",
                LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE, now, now);

        when(employeeService.getEmployee(17L)).thenReturn(response);

        mockMvc.perform(get("/api/employees/17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(17))
                .andExpect(jsonPath("$.employeeCode").value("EMP-1042"))
                .andExpect(jsonPath("$.email").value("asha.rao@example.com"));
    }

    @Test
    void getById_notFound_returns404WithErrorShape() throws Exception {
        when(employeeService.getEmployee(99L)).thenThrow(new EmployeeNotFoundException(99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/employees/99"));
    }
}
