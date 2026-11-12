package com.atlasrh.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max = 30) String employeeNumber,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(max = 120) String position,
        @NotNull @PastOrPresent LocalDate hireDate,
        @NotNull Long departmentId,
        Long managerId) {
}
