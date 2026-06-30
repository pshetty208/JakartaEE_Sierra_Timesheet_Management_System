package sierra.tms.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 *
 * @author prajnashetty
 */
@Entity
@Table(name="Contract")
public class ContractEntity {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable=false)
    private String employeeId;
    
    @Column(name = "supervisor_id", nullable=false)
    private String supervisorId;
    
    @Column(name = "name", nullable=false)
    private String name;
    
    @Column(name = "status", nullable=false)
    private String status;
    
    @Column(name = "start_date", nullable=false)
    private String startDate;
    
    @Column(name = "end_date", nullable=false)
    private String endDate;
    
    @Column(name = "frequency", nullable=false)
    private String frequency;
    
    @Column(name = "hours_per_week", nullable=false)
    private String hoursPerWeek;
    
    @Column(name = "working_days_per_week", nullable=false)
    private String workingDaysPerWeek;
    
    @Column(name = "vacation_days_per_year", nullable=false)
    private String vacationDaysPerYear;
    
    @Column(name = "termination_date", nullable=false)
    private String terminationDate;
    
    @Column(name = "archive_duration", nullable=false)
    private String archiveDuration;

    public ContractEntity() {
    }

    public ContractEntity(Long id, String employeeId, String supervisorId, String name, String status, String startDate, String endDate, String frequency, String hoursPerWeek, String workingDaysPerWeek, String vacationDaysPerYear, String terminationDate, String archiveDuration) {
        this.id = id;
        this.employeeId = employeeId;
        this.supervisorId = supervisorId;
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
    }
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getSupervisorId() {
        return supervisorId;
    }

    public void setSupervisorId(String supervisorId) {
        this.supervisorId = supervisorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getHoursPerWeek() {
        return hoursPerWeek;
    }

    public void setHoursPerWeek(String hoursPerWeek) {
        this.hoursPerWeek = hoursPerWeek;
    }

    public String getWorkingDaysPerWeek() {
        return workingDaysPerWeek;
    }

    public void setWorkingDaysPerWeek(String workingDaysPerWeek) {
        this.workingDaysPerWeek = workingDaysPerWeek;
    }

    public String getVacationDaysPerYear() {
        return vacationDaysPerYear;
    }

    public void setVacationDaysPerYear(String vacationDaysPerYear) {
        this.vacationDaysPerYear = vacationDaysPerYear;
    }

    public String getTerminationDate() {
        return terminationDate;
    }

    public void setTerminationDate(String terminationDate) {
        this.terminationDate = terminationDate;
    }

    public String getArchiveDuration() {
        return archiveDuration;
    }

    public void setArchiveDuration(String archiveDuration) {
        this.archiveDuration = archiveDuration;
    }
    
    

}