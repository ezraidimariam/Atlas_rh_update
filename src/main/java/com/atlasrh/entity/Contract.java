package com.atlasrh.entity;

import com.atlasrh.enums.ContractType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractType type;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    protected Contract() {
    }

    public Contract(ContractType type, LocalDate startDate, LocalDate endDate, BigDecimal grossSalary, Employee employee) {
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.grossSalary = grossSalary;
        this.employee = employee;
    }

    public Long getId() { return id; }
    public ContractType getType() { return type; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BigDecimal getGrossSalary() { return grossSalary; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
}
