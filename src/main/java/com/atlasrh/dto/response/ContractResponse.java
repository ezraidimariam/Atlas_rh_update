package com.atlasrh.dto.response;

import com.atlasrh.enums.ContractType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractResponse(Long id, Long employeeId, ContractType type, LocalDate startDate,
                               LocalDate endDate, BigDecimal grossSalary) {
}
