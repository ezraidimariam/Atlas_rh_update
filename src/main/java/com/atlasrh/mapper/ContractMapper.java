package com.atlasrh.mapper;

import com.atlasrh.dto.response.ContractResponse;
import com.atlasrh.dto.request.ContractRequest;
import com.atlasrh.entity.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "id", ignore = true)
    Contract toEntity(ContractRequest request);

    @Mapping(target = "employeeId", source = "employee.id")
    ContractResponse toResponse(Contract contract);
}
