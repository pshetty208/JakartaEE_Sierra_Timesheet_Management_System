package com.jee.entities;

import com.jee.enums.ContractStatus;
import com.jee.enums.TimesheetFrequency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author pranavsudhir
 */
@Entity
@Table(name = "Contract")
public class ContractEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private RoleEntity employee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private RoleEntity supervisor;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TimesheetFrequency frequency;

    @Column(name = "hours_per_week", nullable = false)
    private double hoursPerWeek;

    @Column(name = "working_days_per_week", nullable = false)
    private int workingDaysPerWeek;

    @Column(name = "vacation_days_per_year", nullable = false)
    private int vacationDaysPerYear;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "archive_duration", nullable = false)
    private int archiveDuration;

    @ManyToMany
    @JoinTable(
            name = "Assistant_Contract",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "assistant_id")
    )
    private Set<RoleEntity> assistants;

    @ManyToMany
    @JoinTable(
            name = "Secretary_Contract",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "secretary_id")
    )
    private Set<RoleEntity> secretaries;

    public ContractEntity() {
        status = ContractStatus.PREPARED;
        workingDaysPerWeek = 5;
        vacationDaysPerYear = 20;
        archiveDuration = 24;
        assistants = new LinkedHashSet<>();
        secretaries = new LinkedHashSet<>();
    }

    public Long getId() {
        return id;
    }

    public RoleEntity getEmployee() {
        return employee;
    }

    public void setEmployee(RoleEntity employee) {
        this.employee = employee;
    }

    public RoleEntity getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(RoleEntity supervisor) {
        this.supervisor = supervisor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public TimesheetFrequency getFrequency() {
        return frequency;
    }

    public void setFrequency(TimesheetFrequency frequency) {
        this.frequency = frequency;
    }

    public double getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(double hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }

    public int getWorkingDaysPerWeek() {
        return workingDaysPerWeek;
    }

    public void setWorkingDaysPerWeek(int workingDaysPerWeek) {
        this.workingDaysPerWeek = workingDaysPerWeek;
    }

    public int getVacationDaysPerYear() {
        return vacationDaysPerYear;
    }

    public void setVacationDaysPerYear(int vacationDaysPerYear) {
        this.vacationDaysPerYear = vacationDaysPerYear;
    }

    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(LocalDate terminationDate) {
        this.terminationDate = terminationDate;
    }

    public int getArchiveDuration() {
        return archiveDuration;
    }

    public void setArchiveDuration(int archiveDuration) {
        this.archiveDuration = archiveDuration;
    }

    public Set<RoleEntity> getAssistants() {
        return assistants;
    }

    public Set<RoleEntity> getSecretaries() {
        return secretaries;
    }
}
