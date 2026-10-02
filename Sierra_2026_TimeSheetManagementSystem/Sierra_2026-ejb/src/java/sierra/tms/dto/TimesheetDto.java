package sierra.tms.dto;

import sierra.tms.utils.enums.TimeSheetStatus;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

public class TimesheetDto implements Serializable {
    public Long id;
    public Long contractId;
    public TimeSheetStatus status;
    public LocalDate startDate;
    public LocalDate endDate;
    public LocalDate signedByEmployee;
    public LocalDate signedBySupervisor;
    public boolean changesRequested;
    public Double hoursDue;
    public List<TimesheetEntryDto> entries;

    public TimesheetDto() {}

    public TimesheetDto(Long id, Long contractId, TimeSheetStatus status, LocalDate startDate,
            LocalDate endDate, LocalDate signedByEmployee, LocalDate signedBySupervisor,
            List<TimesheetEntryDto> entries) {
        this.id = id;
        this.contractId = contractId;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.signedByEmployee = signedByEmployee;
        this.signedBySupervisor = signedBySupervisor;
        this.entries = entries;
    }

    public Long getId() { return id; }
    public Long getContractId() { return contractId; }
    public TimeSheetStatus getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getSignedByEmployee() { return signedByEmployee; }
    public LocalDate getSignedBySupervisor() { return signedBySupervisor; }
    public boolean isChangesRequested() { return changesRequested; }
    public Double getHoursDue() { return hoursDue; }
    public List<TimesheetEntryDto> getEntries() { return entries; }

    public void setId(Long id) { this.id = id; }
    public void setContractId(Long contractId) { this.contractId = contractId; }
    public void setStatus(TimeSheetStatus status) { this.status = status; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setSignedByEmployee(LocalDate signedByEmployee) { this.signedByEmployee = signedByEmployee; }
    public void setSignedBySupervisor(LocalDate signedBySupervisor) { this.signedBySupervisor = signedBySupervisor; }
    public void setChangesRequested(boolean changesRequested) { this.changesRequested = changesRequested; }
    public void setHoursDue(Double hoursDue) { this.hoursDue = hoursDue; }
    public void setEntries(List<TimesheetEntryDto> entries) { this.entries = entries; }
}
