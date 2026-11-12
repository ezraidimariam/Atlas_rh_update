package com.atlasrh.service;

import com.atlasrh.dto.request.LeaveRequestCreate;
import com.atlasrh.entity.Employee;
import com.atlasrh.entity.LeaveRequest;
import com.atlasrh.entity.LeaveType;
import com.atlasrh.enums.RequestStatus;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.exception.ConflictException;
import com.atlasrh.mapper.LeaveRequestMapper;
import com.atlasrh.mapper.LeaveTypeMapper;
import com.atlasrh.repository.EmployeeRepository;
import com.atlasrh.repository.LeaveRequestRepository;
import com.atlasrh.repository.LeaveTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock LeaveTypeRepository leaveTypeRepository;
    @Mock LeaveRequestMapper leaveRequestMapper;
    @Mock LeaveTypeMapper leaveTypeMapper;
    @InjectMocks LeaveRequestService leaveRequestService;

    @Test
    void rejectsOverlappingRequest() {
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(mock(Employee.class)));
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(mock(LeaveType.class)));
        when(leaveRequestRepository.hasOverlappingActiveRequest(3L,
                LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 11))).thenReturn(true);

        assertThrows(ConflictException.class, () -> leaveRequestService.create(3L,
                new LeaveRequestCreate(1L, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 11), "Repos")));
        verifyNoInteractions(leaveRequestMapper);
    }

    @Test
    void requiresACommentToReject() {
        LeaveRequest request = mock(LeaveRequest.class);
        Employee employee = mock(Employee.class);
        Employee manager = mock(Employee.class);
        when(leaveRequestRepository.findById(4L)).thenReturn(Optional.of(request));
        when(request.getEmployee()).thenReturn(employee);
        when(employee.getManager()).thenReturn(manager);
        when(manager.getId()).thenReturn(2L);
        when(request.getStatus()).thenReturn(RequestStatus.EN_ATTENTE);

        assertThrows(BusinessRuleException.class, () -> leaveRequestService.reject(2L, 4L, " "));
        verify(request, never()).setStatus(RequestStatus.REFUSEE);
    }

    @Test
    void onlyTheDirectManagerCanDecideARequest() {
        LeaveRequest request = mock(LeaveRequest.class);
        Employee employee = mock(Employee.class);
        Employee directManager = mock(Employee.class);
        when(leaveRequestRepository.findById(4L)).thenReturn(Optional.of(request));
        when(request.getEmployee()).thenReturn(employee);
        when(employee.getManager()).thenReturn(directManager);
        when(directManager.getId()).thenReturn(5L);

        assertThrows(BusinessRuleException.class, () -> leaveRequestService.approve(2L, 4L));
        assertThrows(BusinessRuleException.class, () -> leaveRequestService.reject(2L, 4L, "Non autorisé"));
        verify(request, never()).setStatus(any());
    }

    @Test
    void employeeCanCancelOnlyTheirPendingRequest() {
        LeaveRequest request = mock(LeaveRequest.class);
        Employee employee = mock(Employee.class);
        when(leaveRequestRepository.findById(4L)).thenReturn(Optional.of(request));
        when(request.getEmployee()).thenReturn(employee);
        when(employee.getId()).thenReturn(3L);
        when(request.getStatus()).thenReturn(RequestStatus.EN_ATTENTE);

        leaveRequestService.cancel(3L, 4L);

        verify(request).setStatus(RequestStatus.ANNULEE);
    }

    @Test
    void rejectsApprovalWhenAnnualBalanceIsInsufficient() {
        LeaveRequest request = mock(LeaveRequest.class);
        Employee employee = mock(Employee.class);
        Employee manager = mock(Employee.class);
        LeaveType leaveType = mock(LeaveType.class);
        when(leaveRequestRepository.findById(4L)).thenReturn(Optional.of(request));
        when(request.getEmployee()).thenReturn(employee);
        when(employee.getManager()).thenReturn(manager);
        when(manager.getId()).thenReturn(2L);
        when(employee.getId()).thenReturn(3L);
        when(request.getStatus()).thenReturn(RequestStatus.EN_ATTENTE);
        when(request.getStartDate()).thenReturn(LocalDate.of(2026, 12, 21));
        when(request.getEndDate()).thenReturn(LocalDate.of(2026, 12, 22));
        when(request.getLeaveType()).thenReturn(leaveType);
        when(employeeRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(employee));
        when(leaveType.getId()).thenReturn(1L);
        when(leaveType.getName()).thenReturn("Congé annuel");
        when(leaveType.getDaysPerYear()).thenReturn(1);
        when(leaveRequestRepository.findApprovedRequestsInYear(3L, 1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(java.util.List.of());

        assertThrows(BusinessRuleException.class, () -> leaveRequestService.approve(2L, 4L));
        verify(request, never()).setStatus(RequestStatus.APPROUVEE);
    }

    @Test
    void approvesRequestWhenEachCoveredYearHasEnoughBalance() {
        LeaveRequest request = mock(LeaveRequest.class);
        Employee employee = mock(Employee.class);
        Employee manager = mock(Employee.class);
        LeaveType leaveType = mock(LeaveType.class);
        when(leaveRequestRepository.findById(4L)).thenReturn(Optional.of(request));
        when(request.getEmployee()).thenReturn(employee);
        when(employee.getManager()).thenReturn(manager);
        when(manager.getId()).thenReturn(2L);
        when(employee.getId()).thenReturn(3L);
        when(request.getStatus()).thenReturn(RequestStatus.EN_ATTENTE);
        when(request.getStartDate()).thenReturn(LocalDate.of(2026, 12, 31));
        when(request.getEndDate()).thenReturn(LocalDate.of(2027, 1, 1));
        when(request.getLeaveType()).thenReturn(leaveType);
        when(employeeRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(employee));
        when(leaveType.getId()).thenReturn(1L);
        when(leaveType.getDaysPerYear()).thenReturn(1);
        when(leaveRequestRepository.findApprovedRequestsInYear(3L, 1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(List.of());
        when(leaveRequestRepository.findApprovedRequestsInYear(3L, 1L,
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 12, 31))).thenReturn(List.of());

        leaveRequestService.approve(2L, 4L);

        verify(request).setStatus(RequestStatus.APPROUVEE);
    }

    @Test
    void refusesRequestWithRequiredComment() {
        LeaveRequest request = mock(LeaveRequest.class);
        Employee employee = mock(Employee.class);
        Employee manager = mock(Employee.class);
        when(leaveRequestRepository.findById(4L)).thenReturn(Optional.of(request));
        when(request.getEmployee()).thenReturn(employee);
        when(employee.getManager()).thenReturn(manager);
        when(manager.getId()).thenReturn(2L);
        when(request.getStatus()).thenReturn(RequestStatus.EN_ATTENTE);

        leaveRequestService.reject(2L, 4L, "Effectif insuffisant");

        verify(request).setStatus(RequestStatus.REFUSEE);
        verify(request).setManagerComment("Effectif insuffisant");
    }

    @Test
    void calculatesBalanceFromApprovedWorkingDays() {
        LeaveType leaveType = mock(LeaveType.class);
        LeaveRequest approved = mock(LeaveRequest.class);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(mock(Employee.class)));
        when(leaveTypeRepository.findAll()).thenReturn(List.of(leaveType));
        when(leaveType.getId()).thenReturn(1L);
        when(leaveType.getName()).thenReturn("Congé annuel");
        when(leaveType.getDaysPerYear()).thenReturn(25);
        when(leaveRequestRepository.findApprovedRequestsInYear(3L, 1L,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))).thenReturn(List.of(approved));
        when(approved.getStartDate()).thenReturn(LocalDate.of(2026, 6, 8));
        when(approved.getEndDate()).thenReturn(LocalDate.of(2026, 6, 10));

        var balance = leaveRequestService.balances(3L, 2026).get(0);

        assertEquals(3, balance.usedDays());
        assertEquals(22, balance.remainingDays());
    }
}
