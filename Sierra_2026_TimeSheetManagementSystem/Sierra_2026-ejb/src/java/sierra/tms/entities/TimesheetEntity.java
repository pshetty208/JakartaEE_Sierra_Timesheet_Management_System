package sierra.tms.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author pranavpanhale
 */
@Entity
@Table(name="TIMESHEET")
public class TimesheetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name="status", length=20, nullable=false)
    private TimesheetStatus status = TimesheetStatus.IN_PROGRESS;

    @Column(name="start_date", nullable=false)
    private LocalDate startDate;

    @Column(name="end_date", nullable=false)
    private LocalDate endDate;

    /** NULL means the timesheet has not been signed by the employee yet. */
    @Column(name="signed_by_employee")
    private LocalDate signedByEmployee;

    /** NULL means the timesheet has not been signed by the supervisor yet. */
    @Column(name="signed_by_supervisor")
    private LocalDate signedBySupervisor;

    // TODO(#29/#30/#31): replace with @ManyToOne ContractEntity once the contract
    // slice lands in sierra.tms.entities. Kept as the raw FK column for now so this
    // slice compiles and is testable on its own.
    @Column(name="contract_id", nullable=false)
    private Long contractId;

    @OneToMany(mappedBy="timesheet", cascade=CascadeType.ALL, orphanRemoval=true)
    private List<TimesheetEntryEntity> entries = new ArrayList<>();

    public TimesheetEntity() {}

    /**
     * Derived (CN4c): hoursDue = (workingDaysInPeriod - publicHolidaysInPeriod)
     * * hoursPerWeek / workingDaysPerWeek.
     *
     * Needs the contract (hoursPerWeek, workingDaysPerWeek) and the public
     * holiday calculator (CN4d), so it stays unimplemented in this slice.
     * Never persisted - the domain model marks it derived.
     */
    @Transient
    public double getHoursDue() {
        throw new UnsupportedOperationException("CN4c not implemented yet");
    }

    public void addEntry(TimesheetEntryEntity entry) {
        entries.add(entry);
        entry.setTimesheet(this);
    }

    public void removeEntry(TimesheetEntryEntity entry) {
        entries.remove(entry);
        entry.setTimesheet(null);
    }

    public Long getId() {
        return id;
    }

    public TimesheetStatus getStatus() {
        return status;
    }

    public void setStatus(TimesheetStatus status) {
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

    public LocalDate getSignedByEmployee() {
        return signedByEmployee;
    }

    public void setSignedByEmployee(LocalDate signedByEmployee) {
        this.signedByEmployee = signedByEmployee;
    }

    public LocalDate getSignedBySupervisor() {
        return signedBySupervisor;
    }

    public void setSignedBySupervisor(LocalDate signedBySupervisor) {
        this.signedBySupervisor = signedBySupervisor;
    }

    public Long getContractId() {
        return contractId;
    }

    public void setContractId(Long contractId) {
        this.contractId = contractId;
    }

    public List<TimesheetEntryEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<TimesheetEntryEntity> entries) {
        this.entries = entries;
    }
}
