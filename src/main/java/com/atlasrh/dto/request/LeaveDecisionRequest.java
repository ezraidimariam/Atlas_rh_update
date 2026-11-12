package com.atlasrh.dto.request;

import jakarta.validation.constraints.Size;

public record LeaveDecisionRequest(@Size(max = 1000) String comment) {
}
