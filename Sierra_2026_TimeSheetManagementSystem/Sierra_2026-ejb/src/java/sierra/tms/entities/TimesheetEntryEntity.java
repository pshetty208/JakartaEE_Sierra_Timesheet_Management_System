package sierra.tms.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 *
 * @author pranavpanhale
 */
@Entity
@Table(name="TIMESHEET_ENTRY")
public class TimesheetEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    @JoinColumn(name="timesheet_id", nullable=false)
    private TimesheetEntity timesheet;

    @Enumerated(EnumType.STRING)
    @Column(name="type", length=20, nullable=false)
    private ReportType type;

    @Column(name="description", length=500)
    private String description;

    @Column(name="entry_date", nullable=false)
    private LocalDate entryDate;

    @Column(name="start_time", nullable=false)
    private LocalTime startTime;

    @Column(name="end_time", nullable=false)
    private LocalTime endTime;

    public TimesheetEntryEntity() {}

    /**
     * Derived: the domain model marks hours as "/hours", so it is computed from
     * startTime and endTime and never stored as a column.
     */
    @Transient
    public double getHours() {
        if (startTime == null || endTime == null) {
            return 0.0;
        }
        return Duration.between(startTime, endTime).toMinutes() / 60.0;
    }

    public Long getId() {
        return id;
    }

    public TimesheetEntity getTimesheet() {
        return timesheet;
    }

    public void setTimesheet(TimesheetEntity timesheet) {
        this.timesheet = timesheet;
    }

    public ReportType getType() {
        return type;
    }

    public void setType(ReportType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
