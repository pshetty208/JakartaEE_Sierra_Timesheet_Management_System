package sierra.tms.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import sierra.tms.utils.ContractStatus;
import sierra.tms.utils.Frequency;


public class ContractDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long employeeId;

    private Long supervisorId;

    private String name;

    private ContractStatus status;

    private LocalDate startDate;

    private Frequency frequency;

    private LocalDate endDate;

    private Integer hoursPerWeek;

    private Integer workingDaysPerWeek;

    private Integer vacationDaysPerYear;

    private LocalDate terminationDate;

    private Integer archiveDuration;

    private Double vacationHours;

    private Set<Long> assistantRoleIds;

    private Set<Long> secretaryRoleIds;


    public ContractDto() {
        assistantRoleIds = new LinkedHashSet<>();
        secretaryRoleIds = new LinkedHashSet<>();
    }


    public ContractDto(
            Long id,
            Long employeeId,
            Long supervisorId,
            String name,
            ContractStatus status,
            LocalDate startDate,
            Frequency frequency,
            LocalDate endDate,
            Integer hoursPerWeek,
            Integer workingDaysPerWeek,
            Integer vacationDaysPerYear,
            LocalDate terminationDate,
            Integer archiveDuration) {

        this.id = id;
        this.employeeId = employeeId;
        this.supervisorId = supervisorId;
        this.name = name;
        this.status = status;
        this.startDate = startDate;
        this.frequency = frequency;
        this.endDate = endDate;
        this.hoursPerWeek = hoursPerWeek;
        this.workingDaysPerWeek = workingDaysPerWeek;
        this.vacationDaysPerYear = vacationDaysPerYear;
        this.terminationDate = terminationDate;
        this.archiveDuration = archiveDuration;
        this.assistantRoleIds = new LinkedHashSet<>();
        this.secretaryRoleIds = new LinkedHashSet<>();
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }


    public Long getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(Long supervisorId) {
        this.supervisorId = supervisorId;
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


    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }


    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }


    public Integer getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(Integer hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }


    public Integer getWorkingDaysPerWeek() {
        return workingDaysPerWeek;
    }

    public void setWorkingDaysPerWeek(Integer workingDaysPerWeek) {
        this.workingDaysPerWeek = workingDaysPerWeek;
    }


    public Integer getVacationDaysPerYear() {
        return vacationDaysPerYear;
    }

    public void setVacationDaysPerYear(Integer vacationDaysPerYear) {
        this.vacationDaysPerYear = vacationDaysPerYear;
    }


    public LocalDate getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(LocalDate terminationDate) {
        this.terminationDate = terminationDate;
    }


    public Integer getArchiveDuration() {
        return archiveDuration;
    }

    public void setArchiveDuration(Integer archiveDuration) {
        this.archiveDuration = archiveDuration;
    }


    public Double getVacationHours() {
        return vacationHours;
    }

    public void setVacationHours(Double vacationHours) {
        this.vacationHours = vacationHours;
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