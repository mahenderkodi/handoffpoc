package com.example.employee.controller;

import com.example.employee.dto.EmployeeCreateRequest;
import com.example.employee.dto.EmployeeResponse;
import com.example.employee.entity.EmployeeStatus;
import com.example.employee.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Only POST /api/employees is implemented in this first slice — list/search, get-by-id,
 * update, and delete (Section 7) will follow in later slices.
 */
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeCreateRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        EmployeeResponse created = employeeService.createEmployee(request);

        URI location = uriBuilder.path("/api/employees/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> list(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "status", required = false) EmployeeStatus status,
            Pageable pageable) {

        Page<EmployeeResponse> page = employeeService.listEmployees(q, status, pageable);
        return ResponseEntity.ok(page);
    }
}
