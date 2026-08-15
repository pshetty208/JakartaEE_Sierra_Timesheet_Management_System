package sierra.tms.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import sierra.tms.utils.enums.TimeSheetStatus;

/**
 *
 * @author pranavpanhale
 */
@Entity
@Table(name = "Timesheet")
public class TimesheetEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TimeSheetStatus status = TimeSheetStatus.IN_PROGRESS;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** NULL means the timesheet has not been signed by the employee yet. */
    @Column(name = "signed_by_employee")
    private LocalDate signedByEmployee;

    /** NULL means the timesheet has not been signed by the supervisor yet. */
    @Column(name = "signed_by_supervisor")
    private LocalDate signedBySupervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private ContractEntity contract;

    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimesheetEntryEntity> entries = new ArrayList<>();

    public TimesheetEntity() {}

    // hoursDue (CN4c) is derived from the contract and the public holiday
    // calendar, so it lives in VacationCalculationService, not on the entity.

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

    public void setId(Long id) {
        this.id = id;
    }

    public TimeSheetStatus getStatus() {
        return status;
    }

    public void setStatus(TimeSheetStatus status) {
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

    public ContractEntity getContract() {
        return contract;
    }

    public void setContract(ContractEntity contract) {
        this.contract = contract;
    }

    public List<TimesheetEntryEntity> getEntries() {
        return entries;
    }

    public void setEntries(List<TimesheetEntryEntity> entries) {
        this.entries = entries;
    }
}
