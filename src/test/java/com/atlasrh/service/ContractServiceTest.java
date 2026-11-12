package com.atlasrh.service;

import com.atlasrh.dto.request.ContractRequest;
import com.atlasrh.enums.ContractType;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.mapper.ContractMapper;
import com.atlasrh.repository.ContractRepository;
import com.atlasrh.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock ContractRepository contractRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock ContractMapper contractMapper;
    @InjectMocks ContractService contractService;

    @Test
    void requiresEndDateForFixedTermContract() {
        ContractRequest request = new ContractRequest(ContractType.CDD, LocalDate.now(), null,
                new BigDecimal("1000"));

        assertThrows(BusinessRuleException.class, () -> contractService.create(1L, request));
        verifyNoInteractions(employeeRepository);
    }
}
