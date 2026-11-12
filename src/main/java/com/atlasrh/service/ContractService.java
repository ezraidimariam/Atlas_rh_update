package com.atlasrh.service;

import com.atlasrh.dto.request.ContractRequest;
import com.atlasrh.dto.response.ContractResponse;
import com.atlasrh.entity.Contract;
import com.atlasrh.entity.Employee;
import com.atlasrh.enums.ContractType;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.exception.ConflictException;
import com.atlasrh.exception.ResourceNotFoundException;
import com.atlasrh.mapper.ContractMapper;
import com.atlasrh.repository.ContractRepository;
import com.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);
    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractMapper contractMapper;

    public ContractService(ContractRepository contractRepository, EmployeeRepository employeeRepository,
                           ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.contractMapper = contractMapper;
    }

    public ContractResponse create(Long employeeId, ContractRequest request) {
        validateDatesAndType(request);
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + employeeId));
        LocalDate today = LocalDate.now();
        boolean newContractIsCurrent = !request.startDate().isAfter(today)
                && (request.endDate() == null || !request.endDate().isBefore(today));
        if (newContractIsCurrent && contractRepository.hasCurrentContract(employeeId, today)) {
            throw new ConflictException("Cet employé possède déjà un contrat en cours.");
        }
        Contract contract = contractMapper.toEntity(request);
        contract.setEmployee(employee);
        Contract saved = contractRepository.save(contract);
        log.info("Contrat créé: id={}, employeeId={}", saved.getId(), employeeId);
        return contractMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ContractResponse> history(Long employeeId, Pageable pageable) {
        getEmployee(employeeId);
        return contractRepository.findByEmployeeIdOrderByStartDateDesc(employeeId, pageable)
                .map(contractMapper::toResponse);
    }

    public ContractResponse terminate(Long contractId, LocalDate endDate) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat introuvable: " + contractId));
        LocalDate today = LocalDate.now();
        if (contract.getEndDate() != null || contract.getStartDate().isAfter(today)) {
            throw new BusinessRuleException("Seul un contrat en cours peut être terminé.");
        }
        if (endDate.isBefore(contract.getStartDate()) || endDate.isAfter(today)) {
            throw new BusinessRuleException("La date de fin doit être comprise entre le début du contrat et aujourd'hui.");
        }
        contract.setEndDate(endDate);
        return contractMapper.toResponse(contract);
    }

    private void validateDatesAndType(ContractRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessRuleException("La date de fin ne peut pas être antérieure à la date de début.");
        }
        if ((request.type() == ContractType.CDD || request.type() == ContractType.STAGE)
                && request.endDate() == null) {
            throw new BusinessRuleException("Un contrat CDD ou STAGE doit avoir une date de fin.");
        }
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
    }
}
