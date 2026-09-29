package sierra.tms.web;

import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.exceptions.TimesheetEntryOverlapException;
import sierra.tms.services.TimesheetService;
import sierra.tms.utils.enums.ReportType;
import sierra.tms.utils.enums.TimeSheetStatus;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


@Named
@ViewScoped
public class TimesheetBean implements Serializable {

    @EJB
    private TimesheetService service;

    private Long selectedTimesheetId;
    private ReportType entryType = ReportType.WORK;
    private String entryDate;
    private String startTime;
    private String endTime;
    private String description;
    private List<TimesheetDto> entryManageableTimesheets;
    private List<TimesheetDto> employeeSignatureRevocableTimesheets;
    private List<TimesheetDto> supervisorSignableTimesheets;
    private List<TimesheetDto> changeRequestableTimesheets;
    private List<TimesheetDto> timesheets;
    private Long timesheetDetailId;
    private TimesheetDto timesheetForPrinting;

    public void addEntry() {

        if (selectedTimesheetId == null) {
            error("Pick a timesheet first.");
            return;
        }

        try {
            TimesheetEntryDto dto = new TimesheetEntryDto();
            dto.setType(entryType);
            dto.setDescription(description);
            dto.setEntryDate(LocalDate.parse(entryDate.trim()));
            dto.setStartTime(LocalTime.parse(startTime.trim()));
            dto.setEndTime(LocalTime.parse(endTime.trim()));

            if (!dto.getEndTime().isAfter(dto.getStartTime())) {
                error("End time must be after start time.");
                return;
            }

            Long id = service.addEntry(selectedTimesheetId, dto);

            if (id == null) {
                error("Timesheet " + selectedTimesheetId + " not found.");
                return;
            }

            refreshTimesheets();
            description = "";
            entryDate = "";
            startTime = "";
            endTime = "";

        } catch (DateTimeParseException e) {
            error("Date must be like 2026-07-06, times like 11:00.");
        } catch (EJBAccessException e) {
            error("You may manage entries only for your own Contract's Timesheets.");
        } catch (EJBException e) {
            if (e.getCause() instanceof TimesheetEntryOverlapException) {
                error(e.getCause().getMessage());
                return;
            }
            throw e;
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
    }

    public void deleteEntry(Long id) {
        try {
            service.deleteEntry(id);
            refreshTimesheets();
        } catch (EJBAccessException e) {
            error("You may manage entries only for your own Contract's Timesheets.");
        }
    }

    public void signTimesheet(Long id) {
        try {
            service.signTimesheet(id);
            refreshTimesheets();
            success("Timesheet signed successfully.");
        } catch (EJBAccessException e) {
            error("Only the assigned employee may sign this timesheet.");
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
    }

    public void revokeSignature(Long id) {
        try {
            service.revokeSignature(id);
            refreshTimesheets();
            success("Employee signature revoked. You can manage entries again.");
        } catch (EJBAccessException e) {
            error("Only the assigned employee may revoke this timesheet signature.");
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
    }

    public void signAsSupervisor(Long id) {
        try {
            service.signAsSupervisor(id);
            refreshTimesheets();
            success("Timesheet signed successfully as supervisor.");
        } catch (EJBAccessException e) {
            error("Only the assigned supervisor may sign this timesheet.");
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
    }

    public void requestChanges(Long id) {
        try {
            service.requestChanges(id);
            refreshTimesheets();
            success("Changes requested. The employee can update the timesheet again.");
        } catch (EJBAccessException e) {
            error("Only the assigned supervisor or assistant may request changes to this timesheet.");
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
    }

    public void archiveTimesheet(Long id) {
        try {
            service.archiveTimesheet(id);
            refreshTimesheets();
            success("Timesheet archived successfully.");
        } catch (EJBAccessException e) {
            error("Only an assigned secretary may archive this timesheet.");
        } catch (IllegalStateException e) {
            error(e.getMessage());
        }
    }

    public void loadTimesheetForPrinting() {
        try {
            timesheetForPrinting = service.getForPrinting(timesheetDetailId);
        } catch (EJBException e) {
            timesheetForPrinting = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "The printable timesheet is temporarily unavailable.",
                            null));
        }
    }

    public List<TimesheetDto> getTimesheets() {
        if (timesheets == null) {
            refreshTimesheets();
        }
        return timesheets;
    }

    public List<TimesheetDto> getEntryManageableTimesheets() {
        if (entryManageableTimesheets == null) {
            entryManageableTimesheets = service.findForEntryManagement();
        }
        return entryManageableTimesheets;
    }

    public List<TimesheetDto> getEmployeeSignatureRevocableTimesheets() {
        if (employeeSignatureRevocableTimesheets == null) {
            employeeSignatureRevocableTimesheets = service.findForEmployeeSignatureRevocation();
        }
        return employeeSignatureRevocableTimesheets;
    }

    public List<TimesheetDto> getSupervisorSignableTimesheets() {
        if (supervisorSignableTimesheets == null) {
            supervisorSignableTimesheets = service.findForSupervisorSigning();
        }
        return supervisorSignableTimesheets;
    }

    public List<TimesheetDto> getChangeRequestableTimesheets() {
        if (changeRequestableTimesheets == null) {
            changeRequestableTimesheets = service.findForChangeRequest();
        }
        return changeRequestableTimesheets;
    }

    public boolean canManageEntries(Long timesheetId) {
        return getEntryManageableTimesheets().stream()
                .anyMatch(timesheet -> timesheet.getId().equals(timesheetId));
    }

    public boolean canSignTimesheet(Long timesheetId) {
        return canManageEntries(timesheetId);
    }

    public boolean canRevokeSignature(Long timesheetId) {
        return getEmployeeSignatureRevocableTimesheets().stream()
                .anyMatch(timesheet -> timesheet.getId().equals(timesheetId));
    }

    public boolean canSignAsSupervisor(Long timesheetId) {
        return getSupervisorSignableTimesheets().stream()
                .anyMatch(timesheet -> timesheet.getId().equals(timesheetId));
    }

    public boolean canRequestChanges(Long timesheetId) {
        return getChangeRequestableTimesheets().stream()
                .anyMatch(timesheet -> timesheet.getId().equals(timesheetId));
    }

    public boolean canArchive(TimesheetDto timesheet) {
        return timesheet != null
                && timesheet.getStatus() == TimeSheetStatus.SIGNED_BY_SUPERVISOR;
    }

    /** Flattened view of every entry, so the page can show them in one table. */
    public List<TimesheetEntryDto> getAllEntries() {

        List<TimesheetEntryDto> all = new ArrayList<>();

        for (TimesheetDto timesheet : getTimesheets()) {
            if (timesheet.getEntries() != null) {
                all.addAll(timesheet.getEntries());
            }
        }

        return all;
    }

    private void refreshTimesheets() {
        timesheets = service.findAll();
        entryManageableTimesheets = null;
        employeeSignatureRevocableTimesheets = null;
        supervisorSignableTimesheets = null;
        changeRequestableTimesheets = null;
    }

    public ReportType[] getReportTypes() {
        return ReportType.values();
    }

    private void error(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    private void success(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
    }

    public Long getSelectedTimesheetId() {
        return selectedTimesheetId;
    }

    public void setSelectedTimesheetId(Long selectedTimesheetId) {
        this.selectedTimesheetId = selectedTimesheetId;
    }

    public ReportType getEntryType() {
        return entryType;
    }

    public void setEntryType(ReportType entryType) {
        this.entryType = entryType;
    }

    public String getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(String entryDate) {
        this.entryDate = entryDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTimesheetDetailId() {
        return timesheetDetailId;
    }

    public void setTimesheetDetailId(Long timesheetDetailId) {
        this.timesheetDetailId = timesheetDetailId;
    }

    public TimesheetDto getTimesheetForPrinting() {
        return timesheetForPrinting;
    }

    public void setTimesheetForPrinting(TimesheetDto timesheetForPrinting) {
        this.timesheetForPrinting = timesheetForPrinting;
    }
}
