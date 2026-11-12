package com.atlasrh.service;

import com.atlasrh.dto.request.DepartmentRequest;
import com.atlasrh.dto.response.DepartmentResponse;
import com.atlasrh.entity.Department;
import com.atlasrh.exception.ConflictException;
import com.atlasrh.exception.ResourceNotFoundException;
import com.atlasrh.mapper.DepartmentMapper;
import com.atlasrh.repository.DepartmentRepository;
import com.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DepartmentService {

    private static final Logger log = LoggerFactory.getLogger(DepartmentService.class);
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentMapper departmentMapper;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository,
                             DepartmentMapper departmentMapper) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
        this.departmentMapper = departmentMapper;
    }

    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Un département portant ce nom existe déjà.");
        }
        Department department = departmentMapper.toEntity(request);
        department.setName(name);
        Department saved = departmentRepository.save(department);
        log.info("Département créé: id={}", saved.getId());
        return departmentMapper.toResponse(saved);
    }

    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = getDepartment(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Un département portant ce nom existe déjà.");
        }
        departmentMapper.update(request, department);
        department.setName(name);
        return departmentMapper.toResponse(department);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(Long id) {
        return departmentMapper.toResponse(getDepartment(id));
    }

    @Transactional(readOnly = true)
    public Page<DepartmentResponse> list(Pageable pageable) {
        return departmentRepository.findAll(pageable).map(departmentMapper::toResponse);
    }

    public void delete(Long id) {
        Department department = getDepartment(id);
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException("Un département qui contient des employés ne peut pas être supprimé.");
        }
        departmentRepository.delete(department);
        log.info("Département supprimé: id={}", id);
    }

    private Department getDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable: " + id));
    }
}
