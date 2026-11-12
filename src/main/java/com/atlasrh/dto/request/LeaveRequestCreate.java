package com.atlasrh.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record LeaveRequestCreate(
        @NotNull Long leaveTypeId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotBlank @Size(max = 1000) String reason) {
}
