package com.atlasrh.dto.response;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String employeeNumber,
        String lastName,
        String firstName,
        String email,
        String position,
        LocalDate hireDate,
        Long departmentId,
        String departmentName,
        Long managerId,
        String managerName) {
}
