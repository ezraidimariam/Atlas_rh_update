package com.atlasrh.mapper;

import com.atlasrh.dto.response.LeaveTypeResponse;
import com.atlasrh.entity.LeaveType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LeaveTypeMapper {
    LeaveTypeResponse toResponse(LeaveType leaveType);
}
