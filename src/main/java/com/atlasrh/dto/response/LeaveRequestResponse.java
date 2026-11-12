package com.atlasrh.dto.response;

import com.atlasrh.enums.RequestStatus;

import java.time.LocalDate;

public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        Long leaveTypeId,
        String leaveTypeName,
        LocalDate startDate,
        LocalDate endDate,
        int workingDays,
        String reason,
        RequestStatus status,
        String managerComment) {
}
