package com.atlasrh.mapper;

import com.atlasrh.dto.request.EmployeeRequest;
import com.atlasrh.dto.response.EmployeeResponse;
import com.atlasrh.entity.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "contracts", ignore = true)
    @Mapping(target = "leaveRequests", ignore = true)
    Employee toEntity(EmployeeRequest request);

    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "team", ignore = true)
    @Mapping(target = "contracts", ignore = true)
    @Mapping(target = "leaveRequests", ignore = true)
    void update(EmployeeRequest request, @MappingTarget Employee employee);

    @Mapping(target = "departmentId", source = "department.id")
    @Mapping(target = "departmentName", source = "department.name")
    @Mapping(target = "managerId", source = "manager.id")
    @Mapping(target = "managerName", expression = "java(employee.getManager() == null ? null : employee.getManager().getFirstName() + \" \" + employee.getManager().getLastName())")
    EmployeeResponse toResponse(Employee employee);
}
