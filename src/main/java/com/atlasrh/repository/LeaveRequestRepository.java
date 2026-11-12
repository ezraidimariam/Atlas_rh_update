package com.atlasrh.repository;

import com.atlasrh.entity.LeaveRequest;
import com.atlasrh.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    Page<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, RequestStatus status, Pageable pageable);
    Page<LeaveRequest> findByEmployeeId(Long employeeId, Pageable pageable);
    Page<LeaveRequest> findByEmployeeManagerIdAndStatus(Long managerId, RequestStatus status, Pageable pageable);

    @Query("""
            select (count(r) > 0) from LeaveRequest r
            where r.employee.id = :employeeId
              and r.status in (com.atlasrh.enums.RequestStatus.EN_ATTENTE, com.atlasrh.enums.RequestStatus.APPROUVEE)
              and r.startDate <= :endDate and r.endDate >= :startDate
            """)
    boolean hasOverlappingActiveRequest(@Param("employeeId") Long employeeId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);

    @Query("""
            select r from LeaveRequest r
            where r.employee.id = :employeeId and r.leaveType.id = :leaveTypeId
              and r.status = com.atlasrh.enums.RequestStatus.APPROUVEE
              and r.startDate <= :yearEnd and r.endDate >= :yearStart
            """)
    java.util.List<LeaveRequest> findApprovedRequestsInYear(@Param("employeeId") Long employeeId,
                                                            @Param("leaveTypeId") Long leaveTypeId,
                                                            @Param("yearStart") LocalDate yearStart,
                                                            @Param("yearEnd") LocalDate yearEnd);
}
