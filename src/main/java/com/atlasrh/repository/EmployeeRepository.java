package com.atlasrh.repository;

import com.atlasrh.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByEmployeeNumberIgnoreCase(String employeeNumber);
    boolean existsByEmployeeNumberIgnoreCaseAndIdNot(String employeeNumber, Long id);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByDepartmentId(Long departmentId);
    List<Employee> findByManagerIdOrderByLastNameAscFirstNameAsc(Long managerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    @Query("""
            select e from Employee e
            where lower(e.firstName) like lower(concat('%', :search, '%'))
               or lower(e.lastName) like lower(concat('%', :search, '%'))
               or lower(concat(e.firstName, ' ', e.lastName)) like lower(concat('%', :search, '%'))
            """)
    Page<Employee> searchByName(@Param("search") String search, Pageable pageable);

    @Query("""
            select e from Employee e
            where e.department.id = :departmentId
              and (lower(e.firstName) like lower(concat('%', :search, '%'))
                or lower(e.lastName) like lower(concat('%', :search, '%'))
                or lower(concat(e.firstName, ' ', e.lastName)) like lower(concat('%', :search, '%')))
            """)
    Page<Employee> searchByNameAndDepartment(@Param("search") String search,
                                             @Param("departmentId") Long departmentId,
                                             Pageable pageable);
}
