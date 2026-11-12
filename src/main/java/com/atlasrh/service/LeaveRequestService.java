package com.atlasrh.service;

import com.atlasrh.dto.request.LeaveRequestCreate;
import com.atlasrh.dto.response.LeaveBalanceResponse;
import com.atlasrh.dto.response.LeaveRequestResponse;
import com.atlasrh.dto.response.LeaveTypeResponse;
import com.atlasrh.dto.response.SeniorityResponse;
import com.atlasrh.entity.Employee;
import com.atlasrh.entity.LeaveRequest;
import com.atlasrh.entity.LeaveType;
import com.atlasrh.enums.RequestStatus;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.exception.ConflictException;
import com.atlasrh.exception.ResourceNotFoundException;
import com.atlasrh.mapper.LeaveRequestMapper;
import com.atlasrh.mapper.LeaveTypeMapper;
import com.atlasrh.repository.EmployeeRepository;
import com.atlasrh.repository.LeaveRequestRepository;
import com.atlasrh.repository.LeaveTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class LeaveRequestService {

    private static final Logger log = LoggerFactory.getLogger(LeaveRequestService.class);
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final LeaveTypeMapper leaveTypeMapper;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository, EmployeeRepository employeeRepository,
                               LeaveTypeRepository leaveTypeRepository, LeaveRequestMapper leaveRequestMapper,
                               LeaveTypeMapper leaveTypeMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestMapper = leaveRequestMapper;
        this.leaveTypeMapper = leaveTypeMapper;
    }

    public LeaveRequestResponse create(Long employeeId, LeaveRequestCreate request) {
        Employee employee = getEmployee(employeeId);
        LeaveType leaveType = getLeaveType(request.leaveTypeId());
        validateDates(request.startDate(), request.endDate());
        if (leaveRequestRepository.hasOverlappingActiveRequest(employeeId, request.startDate(), request.endDate())) {
            throw new ConflictException("Cette période chevauche une autre demande en attente ou approuvée.");
        }
        int workingDays = BusinessDayCalculator.count(request.startDate(), request.endDate());
        if (workingDays == 0) {
            throw new BusinessRuleException("La période demandée ne contient aucun jour ouvré.");
        }
        LeaveRequest leaveRequest = leaveRequestMapper.toEntity(request);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setWorkingDays(workingDays);
        leaveRequest.setStatus(RequestStatus.EN_ATTENTE);
        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        log.info("Demande de congé créée: id={}, employeeId={}", saved.getId(), employeeId);
        return leaveRequestMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> listForEmployee(Long employeeId, RequestStatus status, Pageable pageable) {
        getEmployee(employeeId);
        Page<LeaveRequest> requests = status == null
                ? leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                : leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status, pageable);
        return requests.map(leaveRequestMapper::toResponse);
    }

    public LeaveRequestResponse cancel(Long employeeId, Long requestId) {
        LeaveRequest request = getRequest(requestId);
        if (!request.getEmployee().getId().equals(employeeId)) {
            throw new BusinessRuleException("Seul l'employé concerné peut annuler cette demande.");
        }
        requirePending(request);
        request.setStatus(RequestStatus.ANNULEE);
        return leaveRequestMapper.toResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequestResponse> pendingForManager(Long managerId, Pageable pageable) {
        getEmployee(managerId);
        return leaveRequestRepository.findByEmployeeManagerIdAndStatus(managerId, RequestStatus.EN_ATTENTE, pageable)
                .map(leaveRequestMapper::toResponse);
    }

    public LeaveRequestResponse approve(Long managerId, Long requestId) {
        LeaveRequest request = getRequestForManager(managerId, requestId);
        requirePending(request);
        employeeRepository.findByIdForUpdate(request.getEmployee().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employé introuvable: " + request.getEmployee().getId()));
        ensureSufficientBalance(request);
        request.setStatus(RequestStatus.APPROUVEE);
        request.setManagerComment(null);
        log.info("Demande approuvée: id={}, managerId={}", requestId, managerId);
        return leaveRequestMapper.toResponse(request);
    }

    public LeaveRequestResponse reject(Long managerId, Long requestId, String comment) {
        LeaveRequest request = getRequestForManager(managerId, requestId);
        requirePending(request);
        if (comment == null || comment.isBlank()) {
            throw new BusinessRuleException("Un commentaire est obligatoire pour refuser une demande.");
        }
        request.setStatus(RequestStatus.REFUSEE);
        request.setManagerComment(comment.trim());
        log.info("Demande refusée: id={}, managerId={}", requestId, managerId);
        return leaveRequestMapper.toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> balances(Long employeeId, int year) {
        getEmployee(employeeId);
        List<LeaveBalanceResponse> balances = new ArrayList<>();
        for (LeaveType leaveType : leaveTypeRepository.findAll()) {
            int usedDays = approvedDaysInYear(employeeId, leaveType, year);
            balances.add(new LeaveBalanceResponse(leaveType.getId(), leaveType.getName(), leaveType.getDaysPerYear(),
                    usedDays, leaveType.getDaysPerYear() - usedDays));
        }
        return balances;
    }

    @Transactional(readOnly = true)
    public SeniorityResponse seniority(Long employeeId) {
        Employee employee = getEmployee(employeeId);
        Period seniority = Period.between(employee.getHireDate(), LocalDate.now());
        return new SeniorityResponse(employeeId, seniority.getYears(), seniority.getMonths());
    }

    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> leaveTypes() {
        return leaveTypeRepository.findAll().stream().map(leaveTypeMapper::toResponse).toList();
    }

    private void ensureSufficientBalance(LeaveRequest request) {
        int firstYear = request.getStartDate().getYear();
        int lastYear = request.getEndDate().getYear();
        for (int year = firstYear; year <= lastYear; year++) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);
            LocalDate segmentStart = request.getStartDate().isAfter(yearStart) ? request.getStartDate() : yearStart;
            LocalDate segmentEnd = request.getEndDate().isBefore(yearEnd) ? request.getEndDate() : yearEnd;
            int requestedDays = BusinessDayCalculator.count(segmentStart, segmentEnd);
            int alreadyUsed = approvedDaysInYear(request.getEmployee().getId(), request.getLeaveType(), year);
            if (alreadyUsed + requestedDays > request.getLeaveType().getDaysPerYear()) {
                throw new BusinessRuleException("Le solde de congé " + request.getLeaveType().getName()
                        + " est insuffisant pour l'année " + year + ".");
            }
        }
    }

    private int approvedDaysInYear(Long employeeId, LeaveType leaveType, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        int total = 0;
        for (LeaveRequest approved : leaveRequestRepository.findApprovedRequestsInYear(
                employeeId, leaveType.getId(), yearStart, yearEnd)) {
            LocalDate segmentStart = approved.getStartDate().isAfter(yearStart) ? approved.getStartDate() : yearStart;
            LocalDate segmentEnd = approved.getEndDate().isBefore(yearEnd) ? approved.getEndDate() : yearEnd;
            total += BusinessDayCalculator.count(segmentStart, segmentEnd);
        }
        return total;
    }

    private LeaveRequest getRequestForManager(Long managerId, Long requestId) {
        LeaveRequest request = getRequest(requestId);
        if (request.getEmployee().getManager() == null
                || !request.getEmployee().getManager().getId().equals(managerId)) {
            throw new BusinessRuleException("Seul le manager direct de l'employé peut décider cette demande.");
        }
        return request;
    }

    private void requirePending(LeaveRequest request) {
        if (request.getStatus() != RequestStatus.EN_ATTENTE) {
            throw new BusinessRuleException("Seule une demande en attente peut être modifiée.");
        }
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessRuleException("La date de fin ne peut pas être antérieure à la date de début.");
        }
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
    }

    private LeaveType getLeaveType(Long id) {
        return leaveTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Type de congé introuvable: " + id));
    }

    private LeaveRequest getRequest(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de congé introuvable: " + id));
    }
}
