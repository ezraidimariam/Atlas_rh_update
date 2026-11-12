package com.atlasrh.mapper;

import com.atlasrh.dto.request.DepartmentRequest;
import com.atlasrh.dto.response.DepartmentResponse;
import com.atlasrh.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    @Mapping(target = "employees", ignore = true)
    Department toEntity(DepartmentRequest request);
    DepartmentResponse toResponse(Department department);
    @Mapping(target = "employees", ignore = true)
    void update(DepartmentRequest request, @MappingTarget Department department);
}
