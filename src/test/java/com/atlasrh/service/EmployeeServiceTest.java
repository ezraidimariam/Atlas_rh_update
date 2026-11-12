package com.atlasrh.service;

import com.atlasrh.dto.request.EmployeeRequest;
import com.atlasrh.entity.Department;
import com.atlasrh.entity.Employee;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.mapper.EmployeeMapper;
import com.atlasrh.repository.DepartmentRepository;
import com.atlasrh.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock EmployeeRepository employeeRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock EmployeeMapper employeeMapper;
    @InjectMocks EmployeeService employeeService;

    @Test
    void rejectsDuplicateEmployeeNumber() {
        EmployeeRequest request = request(1L, null);
        when(employeeRepository.existsByEmployeeNumberIgnoreCase("RH001")).thenReturn(true);

        org.junit.jupiter.api.Assertions.assertThrows(com.atlasrh.exception.ConflictException.class,
                () -> employeeService.create(request));
        verifyNoInteractions(employeeMapper);
    }

    @Test
    void preventsEmployeeFromBecomingTheirOwnManager() {
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(mock(Employee.class)));
        when(employeeRepository.existsByEmployeeNumberIgnoreCaseAndIdNot("RH001", 7L)).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("rh@example.test", 7L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mock(Department.class)));

        assertThrows(BusinessRuleException.class, () -> employeeService.update(7L, request(1L, 7L)));
    }

    @Test
    void preventsCircularManagerHierarchy() {
        Employee employee = mock(Employee.class);
        Employee manager = mock(Employee.class);
        Employee intermediateManager = mock(Employee.class);
        when(employee.getId()).thenReturn(7L);
        when(manager.getId()).thenReturn(8L);
        when(intermediateManager.getId()).thenReturn(9L);
        when(manager.getManager()).thenReturn(intermediateManager);
        when(intermediateManager.getManager()).thenReturn(employee);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(employee));
        when(employeeRepository.findById(8L)).thenReturn(Optional.of(manager));
        when(employeeRepository.existsByEmployeeNumberIgnoreCaseAndIdNot("RH001", 7L)).thenReturn(false);
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("rh@example.test", 7L)).thenReturn(false);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mock(Department.class)));

        assertThrows(BusinessRuleException.class, () -> employeeService.update(7L, request(1L, 8L)));
    }

    private EmployeeRequest request(Long departmentId, Long managerId) {
        return new EmployeeRequest("RH001", "Nom", "Prenom", "rh@example.test", "RH",
                LocalDate.of(2022, 1, 1), departmentId, managerId);
    }
}
