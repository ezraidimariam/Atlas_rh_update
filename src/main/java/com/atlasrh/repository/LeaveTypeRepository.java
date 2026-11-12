package com.atlasrh.repository;

import com.atlasrh.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
}
