package com.atlasrh.entity;

import com.atlasrh.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "working_days", nullable = false)
    private int workingDays;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(name = "manager_comment", length = 1000)
    private String managerComment;

    protected LeaveRequest() {
    }

    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate startDate, LocalDate endDate,
                        int workingDays, String reason) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.workingDays = workingDays;
        this.reason = reason;
        this.status = RequestStatus.EN_ATTENTE;
    }

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public LeaveType getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public int getWorkingDays() { return workingDays; }
    public String getReason() { return reason; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus status) { this.status = status; }
    public String getManagerComment() { return managerComment; }
    public void setManagerComment(String managerComment) { this.managerComment = managerComment; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }
    public void setWorkingDays(int workingDays) { this.workingDays = workingDays; }
}
