package sierra.tms.web;

import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.services.TimesheetService;
import sierra.tms.utils.enums.ReportType;
import sierra.tms.utils.enums.TimeSheetStatus;
import jakarta.ejb.EJB;
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

/**
 * Drives the timesheet CRUD slice from the browser. Deliberately plain, like
 * UserBean - PrimeFaces (UI4) can replace the raw inputs later.
 */
@Named
@ViewScoped
public class TimesheetBean implements Serializable {

    @EJB
    private TimesheetService service;

    private String contractId;
    private String startDate;
    private String endDate;

    private Long selectedTimesheetId;
    private ReportType entryType = ReportType.WORK;
    private String entryDate;
    private String startTime;
    private String endTime;
    private String description;

    private TimesheetDto printableTimesheet;

    public void saveTimesheet() {

        try {
            TimesheetDto dto = new TimesheetDto();
            dto.setContractId(Long.valueOf(contractId.trim()));
            dto.setStartDate(LocalDate.parse(startDate.trim()));
            dto.setEndDate(LocalDate.parse(endDate.trim()));

            if (dto.getEndDate().isBefore(dto.getStartDate())) {
                error("End date is before start date.");
                return;
            }

            Long id = service.save(dto);

            if (id == null) {
                error("Contract " + dto.getContractId() + " not found.");
                return;
            }

            info("Created timesheet " + id + ".");
            contractId = "";
            startDate = "";
            endDate = "";

        } catch (NumberFormatException e) {
            error("Contract must be a number.");
        } catch (DateTimeParseException e) {
            error("Dates must be in ISO form, e.g. 2026-07-01.");
        }
    }

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

            info("Added entry " + id + ".");
            description = "";
            entryDate = "";
            startTime = "";
            endTime = "";

        } catch (DateTimeParseException e) {
            error("Date must be like 2026-07-06, times like 11:00.");
        }
    }

    public void deleteTimesheet(Long id) {
        run(() -> service.delete(id), "Deleted timesheet " + id + " and its entries.");
    }

    public void deleteEntry(Long id) {
        run(() -> service.deleteEntry(id), "Deleted entry " + id + ".");
    }

    public List<TimesheetDto> getTimesheets() {
        return service.findAll();
    }

    /** Flattened view of every entry, so the page can show them in one table. */
    public List<TimesheetEntryDto> getAllEntries() {

        List<TimesheetEntryDto> all = new ArrayList<>();

        for (TimesheetDto timesheet : service.findAll()) {
            if (timesheet.getEntries() != null) {
                all.addAll(timesheet.getEntries());
            }
        }

        return all;
    }

    public ReportType[] getReportTypes() {
        return ReportType.values();
    }

    public void signTimesheet(Long id) {
        run(() -> service.signTimesheet(id), "Signed timesheet " + id + ".");
    }

    public void revokeSignature(Long id) {
        run(() -> service.revokeSignature(id), "Revoked the signature on timesheet " + id + ".");
    }

    public void signAsSupervisor(Long id) {
        run(() -> service.signAsSupervisor(id), "Countersigned timesheet " + id + ".");
    }

    public void requestChanges(Long id) {
        run(() -> service.requestChanges(id), "Requested changes on timesheet " + id + ".");
    }

    public void archiveTimesheet(Long id) {
        run(() -> service.archiveTimesheet(id), "Archived timesheet " + id + ".");
    }

    public void printTimesheet(Long id) {
        try {
            printableTimesheet = service.getForPrinting(id);
        } catch (EJBException exception) {
            printableTimesheet = null;
            error(reasonOf(exception));
        }
    }

    public TimesheetDto getPrintableTimesheet() {
        return printableTimesheet;
    }

    public String statusLabel(TimeSheetStatus status) {
        return switch (status) {
            case IN_PROGRESS -> "In progress";
            case SIGNED_BY_EMPLOYEE -> "Signed by employee";
            case SIGNED_BY_SUPERVISOR -> "Signed by supervisor";
            case ARCHIVED -> "Archived";
        };
    }

    public String statusStyleClass(TimeSheetStatus status) {
        return "timesheet-overview__status--" + status.name().toLowerCase().replace('_', '-');
    }

    private void run(Runnable action, String successMessage) {
        try {
            action.run();
            info(successMessage);
        } catch (EJBException exception) {
            error(reasonOf(exception));
        }
    }

    /** The container wraps the service exception, so the reason sits on the cause. */
    private String reasonOf(EJBException exception) {
        Throwable cause = exception.getCause();
        return cause == null || cause.getMessage() == null
                ? "The action could not be completed."
                : cause.getMessage();
    }

    private void info(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, message, null));
    }

    private void error(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
    }

    public String getContractId() {
        return contractId;
    }

    public void setContractId(String contractId) {
        this.contractId = contractId;
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
}
