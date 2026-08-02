package sierra.tms.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import sierra.tms.utils.ContractStatus;
import sierra.tms.utils.Frequency;

@Entity
@Table(name = "Contract")
public class ContractEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private PersonEntity employee;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private PersonEntity supervisor;


    @Column(name = "name", nullable = false, length = 100)
    private String name;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status = ContractStatus.PREPARED;


    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;


    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;


    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private Frequency frequency;


    @Column(name = "hours_per_week", nullable = false)
    private Integer hoursPerWeek;


    @Column(name = "working_days_per_week", nullable = false)
    private Integer workingDaysPerWeek = 5;


    @Column(name = "vacation_days_per_year", nullable = false)
    private Integer vacationDaysPerYear = 20;


    @Column(name = "termination_date")
    private LocalDate terminationDate;


    @Column(name = "archive_duration", nullable = false)
    private Integer archiveDuration = 24;


    @OneToMany
    @JoinTable(
            name = "Assistant_Contract",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "assistant_id")
    )
    private Set<PersonEntity> assistants = new LinkedHashSet<>();


    @OneToMany
    @JoinTable(
            name = "Secretary_Contract",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "secretary_id")
    )
    private Set<PersonEntity> secretaries = new LinkedHashSet<>();


    public ContractEntity() {
    }


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public PersonEntity getEmployee() {
        return employee;
    }


    public void setEmployee(PersonEntity employee) {
        this.employee = employee;
    }


    public PersonEntity getSupervisor() {
        return supervisor;
    }


    public void setSupervisor(PersonEntity supervisor) {
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


    public Frequency getFrequency() {
        return frequency;
    }


    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
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


    public Set<PersonEntity> getAssistants() {
        return assistants;
    }


    public void setAssistants(Set<PersonEntity> assistants) {
        this.assistants = assistants;
    }


    public Set<PersonEntity> getSecretaries() {
        return secretaries;
    }


    public void setSecretaries(Set<PersonEntity> secretaries) {
        this.secretaries = secretaries;
    }
}