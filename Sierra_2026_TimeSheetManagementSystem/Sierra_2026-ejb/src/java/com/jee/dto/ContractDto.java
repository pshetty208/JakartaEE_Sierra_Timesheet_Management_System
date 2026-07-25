package com.jee.dto;

import com.jee.enums.ContractStatus;
import com.jee.enums.TimesheetFrequency;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author pranavsudhir
 */
public class ContractDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long employeeRoleId;
    private Long supervisorRoleId;
    private String name;
    private ContractStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private TimesheetFrequency frequency;
    private double hoursPerWeek;
    private int workingDaysPerWeek;
    private int vacationDaysPerYear;
    private LocalDate terminationDate;
    private int archiveDuration;
    private Set<Long> assistantRoleIds;
    private Set<Long> secretaryRoleIds;

    public ContractDto() {
        assistantRoleIds = new LinkedHashSet<>();
        secretaryRoleIds = new LinkedHashSet<>();
    }

    public ContractDto(
            Long id,
            Long employeeRoleId,
            Long supervisorRoleId,
            String name,
            ContractStatus status,
            LocalDate startDate,
            LocalDate endDate,
            TimesheetFrequency frequency,
            double hoursPerWeek,
            int workingDaysPerWeek,
            int vacationDaysPerYear,
            LocalDate terminationDate,
            int archiveDuration,
            Set<Long> assistantRoleIds,
            Set<Long> secretaryRoleIds) {
        this.id = id;
        this.employeeRoleId = employeeRoleId;
        this.supervisorRoleId = supervisorRoleId;
        this.name = name;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.frequency = frequency;
        this.hoursPerWeek = hoursPerWeek;
        this.workingDaysPerWeek = workingDaysPerWeek;
        this.vacationDaysPerYear = vacationDaysPerYear;
        this.terminationDate = terminationDate;
        this.archiveDuration = archiveDuration;
        this.assistantRoleIds = assistantRoleIds;
        this.secretaryRoleIds = secretaryRoleIds;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeRoleId() {
        return employeeRoleId;
    }

    public void setEmployeeRoleId(Long employeeRoleId) {
        this.employeeRoleId = employeeRoleId;
    }

    public Long getSupervisorRoleId() {
        return supervisorRoleId;
    }

    public void setSupervisorRoleId(Long supervisorRoleId) {
        this.supervisorRoleId = supervisorRoleId;
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

    public Set<Long> getAssistantRoleIds() {
        return assistantRoleIds;
    }

    public void setAssistantRoleIds(Set<Long> assistantRoleIds) {
        this.assistantRoleIds = assistantRoleIds;
    }

    public Set<Long> getSecretaryRoleIds() {
        return secretaryRoleIds;
    }

    public void setSecretaryRoleIds(Set<Long> secretaryRoleIds) {
        this.secretaryRoleIds = secretaryRoleIds;
    }
}
