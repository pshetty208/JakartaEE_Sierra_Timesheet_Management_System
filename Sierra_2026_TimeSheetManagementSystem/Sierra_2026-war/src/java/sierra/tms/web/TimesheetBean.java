package sierra.tms.web;

import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.exceptions.TimesheetEntryOverlapException;
import sierra.tms.i18n.UiMessages;
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
            error(UiMessages.get("timesheet.message.selectFirst"));
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
                error(UiMessages.get("timesheet.message.endAfterStart"));
                return;
            }

            Long id = service.addEntry(selectedTimesheetId, dto);

            if (id == null) {
                error(UiMessages.get("timesheet.message.notFound", selectedTimesheetId));
                return;
            }

            refreshTimesheets();
            description = "";
            entryDate = "";
            startTime = "";
            endTime = "";

        } catch (DateTimeParseException e) {
            error(UiMessages.get("timesheet.message.invalidDateTime"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.ownEntriesOnly"));
        } catch (EJBException e) {
            if (e.getCause() instanceof TimesheetEntryOverlapException) {
                error(UiMessages.get("timesheet.message.entryOverlap"));
                return;
            }
            throw e;
        } catch (IllegalStateException e) {
            error(UiMessages.get("timesheet.message.entryUnavailable"));
        }
    }

    public void deleteEntry(Long id) {
        try {
            service.deleteEntry(id);
            refreshTimesheets();
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.ownEntriesOnly"));
        }
    }

    public void signTimesheet(Long id) {
        try {
            service.signTimesheet(id);
            refreshTimesheets();
            success(UiMessages.get("timesheet.message.signed"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.employeeSignOnly"));
        } catch (IllegalStateException e) {
            error(UiMessages.get("timesheet.message.signUnavailable"));
        }
    }

    public void revokeSignature(Long id) {
        try {
            service.revokeSignature(id);
            refreshTimesheets();
            success(UiMessages.get("timesheet.message.signatureRevoked"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.employeeRevokeOnly"));
        } catch (IllegalStateException e) {
            error(UiMessages.get("timesheet.message.revokeUnavailable"));
        }
    }

    public void signAsSupervisor(Long id) {
        try {
            service.signAsSupervisor(id);
            refreshTimesheets();
            success(UiMessages.get("timesheet.message.supervisorSigned"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.supervisorSignOnly"));
        } catch (IllegalStateException e) {
            error(UiMessages.get("timesheet.message.supervisorSignUnavailable"));
        }
    }

    public void requestChanges(Long id) {
        try {
            service.requestChanges(id);
            refreshTimesheets();
            success(UiMessages.get("timesheet.message.changesRequested"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.requestChangesOnly"));
        } catch (IllegalStateException e) {
            error(UiMessages.get("timesheet.message.requestChangesUnavailable"));
        }
    }

    public void archiveTimesheet(Long id) {
        try {
            service.archiveTimesheet(id);
            refreshTimesheets();
            success(UiMessages.get("timesheet.message.archived"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.archiveOnly"));
        } catch (IllegalStateException e) {
            error(UiMessages.get("timesheet.message.archiveUnavailable"));
        }
    }

    public void loadTimesheetForPrinting() {
        try {
            timesheetForPrinting = service.getForPrinting(timesheetDetailId);
        } catch (EJBException e) {
            timesheetForPrinting = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            UiMessages.get("timesheet.message.printUnavailable"),
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

    public String reportTypeLabel(ReportType reportType) {
        return UiMessages.get(reportType.name());
    }

    public String statusLabel(TimeSheetStatus status) {
        return UiMessages.get(status.name());
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
