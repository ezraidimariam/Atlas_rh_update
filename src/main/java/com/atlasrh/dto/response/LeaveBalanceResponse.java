package com.atlasrh.dto.response;

public record LeaveBalanceResponse(Long leaveTypeId, String leaveTypeName, int daysPerYear, int usedDays,
                                   int remainingDays) {
}
