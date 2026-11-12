package com.atlasrh.dto.request;

import com.atlasrh.enums.ContractType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(
        @NotNull ContractType type,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal grossSalary) {
}
