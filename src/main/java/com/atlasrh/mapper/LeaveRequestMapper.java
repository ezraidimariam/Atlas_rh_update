package com.atlasrh.mapper;

import com.atlasrh.dto.request.LeaveRequestCreate;
import com.atlasrh.dto.response.LeaveRequestResponse;
import com.atlasrh.entity.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workingDays", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "managerComment", ignore = true)
    LeaveRequest toEntity(LeaveRequestCreate request);

    @Mapping(target = "employeeId", source = "employee.id")
    @Mapping(target = "leaveTypeId", source = "leaveType.id")
    @Mapping(target = "leaveTypeName", source = "leaveType.name")
    LeaveRequestResponse toResponse(LeaveRequest request);
}
