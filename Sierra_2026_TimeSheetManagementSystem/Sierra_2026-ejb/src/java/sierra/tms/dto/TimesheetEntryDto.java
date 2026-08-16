package sierra.tms.dto;

import sierra.tms.utils.enums.ReportType;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

public class TimesheetEntryDto implements Serializable {
    public Long id;
    public Long timesheetId;
    public ReportType type;
    public String description;
    public LocalDate entryDate;
    public LocalTime startTime;
    public LocalTime endTime;
    public double hours;

    public TimesheetEntryDto() {}

    public TimesheetEntryDto(Long id, Long timesheetId, ReportType type, String description,
            LocalDate entryDate, LocalTime startTime, LocalTime endTime, double hours) {
        this.id = id;
        this.timesheetId = timesheetId;
        this.type = type;
        this.description = description;
        this.entryDate = entryDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hours = hours;
    }

    public Long getId() { return id; }
    public Long getTimesheetId() { return timesheetId; }
    public ReportType getType() { return type; }
    public String getDescription() { return description; }
    public LocalDate getEntryDate() { return entryDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public double getHours() { return hours; }

    public void setId(Long id) { this.id = id; }
    public void setTimesheetId(Long timesheetId) { this.timesheetId = timesheetId; }
    public void setType(ReportType type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public void setHours(double hours) { this.hours = hours; }
}
