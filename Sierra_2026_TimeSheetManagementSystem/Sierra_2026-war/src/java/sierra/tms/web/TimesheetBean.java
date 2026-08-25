package sierra.tms.web;

import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.exceptions.TimesheetEntryOverlapException;
import sierra.tms.services.TimesheetService;
import sierra.tms.utils.enums.ReportType;
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
    private TimesheetDto printableTimesheet;
    private List<TimesheetDto> entryManageableTimesheets;
    private List<TimesheetDto> timesheets;

//Check if needed - before deleting
//    public void saveTimesheet() {
//
//        try {
//            TimesheetDto dto = new TimesheetDto();
//            dto.setContractId(Long.valueOf(contractId.trim()));
//            dto.setStartDate(LocalDate.parse(startDate.trim()));
//            dto.setEndDate(LocalDate.parse(endDate.trim()));
//
//            if (dto.getEndDate().isBefore(dto.getStartDate())) {
//                error("End date is before start date.");
//                return;
//            }
//
//            Long id = service.save(dto);
//
//            if (id == null) {
//                error("Contract " + dto.getContractId() + " not found.");
//                return;
//            }
//
//            info("Created timesheet " + id + ".");
//            contractId = "";
//            startDate = "";
//            endDate = "";
//
//        } catch (NumberFormatException e) {
//            error("Contract must be a number.");
//        } catch (DateTimeParseException e) {
//            error("Dates must be in ISO form, e.g. 2026-07-01.");
//        }
//    }

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

    public boolean canManageEntries(Long timesheetId) {
        return getEntryManageableTimesheets().stream()
                .anyMatch(timesheet -> timesheet.getId().equals(timesheetId));
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
    }

    public ReportType[] getReportTypes() {
        return ReportType.values();
    }

    private void error(String message) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, null));
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
