package com.atlasrh.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "leave_types", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class LeaveType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "days_per_year", nullable = false)
    private int daysPerYear;

    protected LeaveType() {
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDaysPerYear() { return daysPerYear; }
}
