package com.atlasrh.service;

import com.atlasrh.dto.request.DepartmentRequest;
import com.atlasrh.exception.ConflictException;
import com.atlasrh.mapper.DepartmentMapper;
import com.atlasrh.repository.DepartmentRepository;
import com.atlasrh.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock DepartmentMapper departmentMapper;
    @InjectMocks DepartmentService departmentService;

    @Test
    void rejectsDuplicateName() {
        when(departmentRepository.existsByNameIgnoreCase("Informatique")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> departmentService.create(new DepartmentRequest("Informatique", "IT")));
        verifyNoInteractions(departmentMapper);
    }

    @Test
    void refusesDeletingDepartmentWithEmployees() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(mock(com.atlasrh.entity.Department.class)));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> departmentService.delete(1L));
        verify(departmentRepository, never()).delete(any());
    }
}
