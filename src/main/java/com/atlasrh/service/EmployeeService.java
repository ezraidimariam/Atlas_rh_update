package com.atlasrh.service;

import com.atlasrh.dto.request.EmployeeRequest;
import com.atlasrh.dto.response.EmployeeResponse;
import com.atlasrh.entity.Department;
import com.atlasrh.entity.Employee;
import com.atlasrh.exception.BusinessRuleException;
import com.atlasrh.exception.ConflictException;
import com.atlasrh.exception.ResourceNotFoundException;
import com.atlasrh.mapper.EmployeeMapper;
import com.atlasrh.repository.DepartmentRepository;
import com.atlasrh.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                           EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.employeeMapper = employeeMapper;
    }

    public EmployeeResponse create(EmployeeRequest request) {
        checkUniqueFields(request, null);
        Employee employee = employeeMapper.toEntity(request);
        employee.setDepartment(getDepartment(request.departmentId()));
        employee.setManager(getManager(request.managerId(), null));
        Employee saved = employeeRepository.save(employee);
        log.info("Employé créé: id={}", saved.getId());
        return employeeMapper.toResponse(saved);
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = getEmployee(id);
        checkUniqueFields(request, id);
        Department department = getDepartment(request.departmentId());
        Employee manager = getManager(request.managerId(), id);
        employeeMapper.update(request, employee);
        employee.setDepartment(department);
        employee.setManager(manager);
        return employeeMapper.toResponse(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        return employeeMapper.toResponse(getEmployee(id));
    }

    @Transactional(readOnly = true)
    public Page<EmployeeResponse> search(String search, Long departmentId, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        Page<Employee> employees;
        if (normalizedSearch == null && departmentId == null) {
            employees = employeeRepository.findAll(pageable);
        } else if (normalizedSearch == null) {
            employees = employeeRepository.findByDepartmentId(departmentId, pageable);
        } else if (departmentId == null) {
            employees = employeeRepository.searchByName(normalizedSearch, pageable);
        } else {
            employees = employeeRepository.searchByNameAndDepartment(normalizedSearch, departmentId, pageable);
        }
        return employees.map(employeeMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> getTeam(Long managerId) {
        getEmployee(managerId);
        return employeeRepository.findByManagerIdOrderByLastNameAscFirstNameAsc(managerId)
                .stream().map(employeeMapper::toResponse).toList();
    }

    private void checkUniqueFields(EmployeeRequest request, Long currentId) {
        boolean numberExists = currentId == null
                ? employeeRepository.existsByEmployeeNumberIgnoreCase(request.employeeNumber())
                : employeeRepository.existsByEmployeeNumberIgnoreCaseAndIdNot(request.employeeNumber(), currentId);
        boolean emailExists = currentId == null
                ? employeeRepository.existsByEmailIgnoreCase(request.email())
                : employeeRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), currentId);
        if (numberExists) {
            throw new ConflictException("Ce matricule est déjà utilisé.");
        }
        if (emailExists) {
            throw new ConflictException("Cette adresse e-mail est déjà utilisée.");
        }
    }

    private Department getDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Département introuvable: " + id));
    }

    private Employee getManager(Long managerId, Long employeeId) {
        if (managerId == null) {
            return null;
        }
        if (employeeId != null && employeeId.equals(managerId)) {
            throw new BusinessRuleException("Un employé ne peut pas être son propre manager.");
        }
        Employee manager = getEmployee(managerId);
        if (employeeId != null) {
            Set<Long> visited = new HashSet<>();
            Employee ancestor = manager;
            while (ancestor != null) {
                Long ancestorId = ancestor.getId();
                if (employeeId.equals(ancestorId) || !visited.add(ancestorId)) {
                    throw new BusinessRuleException("Cette relation créerait une hiérarchie circulaire.");
                }
                ancestor = ancestor.getManager();
            }
        }
        return manager;
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
    }
}
