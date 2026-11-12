package com.atlasrh.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContractEndRequest(@NotNull LocalDate endDate) {
}
