package com.example.employee.controller;

import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerListTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void list_withQuery_returnsPagedJson() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        EmployeeResponse resp = new EmployeeResponse(17L, "EMP-1", "Asha", "Rao",
                "asha.rao@example.com", "+91-98765-43210", "Engineering", "Backend",
                LocalDate.of(2024, 3, 1), EmployeeStatus.ACTIVE, now, now);

        when(employeeService.listEmployees(eq("Asha"), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/employees").param("q", "Asha").param("page", "0").param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-1"));
    }
}
