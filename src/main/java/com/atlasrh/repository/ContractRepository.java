package com.atlasrh.repository;

import com.atlasrh.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    Page<Contract> findByEmployeeIdOrderByStartDateDesc(Long employeeId, Pageable pageable);

    @Query("""
            select (count(c) > 0) from Contract c
            where c.employee.id = :employeeId and c.startDate <= :today
              and (c.endDate is null or c.endDate >= :today)
            """)
    boolean hasCurrentContract(@Param("employeeId") Long employeeId, @Param("today") LocalDate today);
}
