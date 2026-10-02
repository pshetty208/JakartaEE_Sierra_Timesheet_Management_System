package sierra.tms.web;

import sierra.tms.dto.ContractDto;
import sierra.tms.dto.PersonDto;
import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.i18n.UiMessages;
import sierra.tms.services.ContractService;
import sierra.tms.services.PersonService;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;


@Named
@ViewScoped
public class TimesheetBean implements Serializable {

    private static final DateTimeFormatter SEARCH_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @EJB
    private TimesheetService service;

    @EJB
    private ContractService contractService;

    @EJB
    private PersonService personService;

    private String searchTerm;
    private Map<Long, ContractDto> contractsById;
    private Map<Long, PersonDto> peopleById;

    private Long selectedTimesheetId;
    private ReportType entryType = ReportType.WORK;
    private String entryDate;
    private String startTime;
    private String endTime;
    private String description;
    private TimesheetDto printableTimesheet;
    private List<TimesheetDto> entryManageableTimesheets;
    private List<TimesheetDto> employeeSignatureRevocableTimesheets;
    private List<TimesheetDto> supervisorSignableTimesheets;
    private List<TimesheetDto> changeRequestableTimesheets;
    private List<TimesheetDto> timesheets;
    private Long timesheetDetailId;
    private TimesheetDto timesheetForPrinting;

    private Long editingEntryId;
    private ReportType editEntryType = ReportType.WORK;
    private String editEntryDate;
    private String editStartTime;
    private String editEndTime;
    private String editDescription;

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
            error(UiMessages.get("timesheet.message.selectFirst"));
            return;
        }

        if (entryDate == null || entryDate.isBlank()
                || startTime == null || startTime.isBlank()
                || endTime == null || endTime.isBlank()) {
            error(UiMessages.get("timesheet.message.invalidDateTime"));
            return;
        }

        try {
            TimesheetEntryDto dto = new TimesheetEntryDto();
            dto.setType(entryType);
            dto.setDescription(description);
            dto.setEntryDate(LocalDate.parse(entryDate.trim()));
            dto.setStartTime(parseTime(startTime));
            dto.setEndTime(parseTime(endTime));

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
            error(UiMessages.get("timesheet.message.entryUnavailable"));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("This entry overlaps")) {
                error(UiMessages.get("timesheet.message.entryOverlap"));
            } else if (e.getMessage() != null && e.getMessage().startsWith("This entry date is outside")) {
                error(UiMessages.get("timesheet.message.entryOutOfRange"));
            } else {
                error(UiMessages.get("timesheet.message.entryUnavailable"));
            }
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "add timesheet entry",
                    "common.error.operationFailed", e);
        }
    }

    public void editEntry(TimesheetEntryDto entry) {
        editingEntryId = entry.getId();
        editEntryType = entry.getType();
        editEntryDate = entry.getEntryDate().toString();
        editStartTime = entry.getStartTime().toString();
        editEndTime = entry.getEndTime().toString();
        editDescription = entry.getDescription();
    }

    public void updateEntry() {

        if (editEntryDate == null || editEntryDate.isBlank()
                || editStartTime == null || editStartTime.isBlank()
                || editEndTime == null || editEndTime.isBlank()) {
            error(UiMessages.get("timesheet.message.invalidDateTime"));
            return;
        }

        try {
            TimesheetEntryDto dto = new TimesheetEntryDto();
            dto.setId(editingEntryId);
            dto.setType(editEntryType);
            dto.setDescription(editDescription);
            dto.setEntryDate(LocalDate.parse(editEntryDate.trim()));
            dto.setStartTime(parseTime(editStartTime));
            dto.setEndTime(parseTime(editEndTime));

            if (!dto.getEndTime().isAfter(dto.getStartTime())) {
                error(UiMessages.get("timesheet.message.endAfterStart"));
                return;
            }

            service.updateEntry(dto);
            refreshTimesheets();
            cancelEdit();

        } catch (DateTimeParseException e) {
            error(UiMessages.get("timesheet.message.invalidDateTime"));
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.ownEntriesOnly"));
        } catch (EJBException e) {
            error(UiMessages.get("timesheet.message.entryUnavailable"));
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("This entry overlaps")) {
                error(UiMessages.get("timesheet.message.entryOverlap"));
            } else if (e.getMessage() != null && e.getMessage().startsWith("This entry date is outside")) {
                error(UiMessages.get("timesheet.message.entryOutOfRange"));
            } else {
                error(UiMessages.get("timesheet.message.entryUnavailable"));
            }
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "update timesheet entry",
                    "common.error.operationFailed", e);
        }
    }

    public void cancelEdit() {
        editingEntryId = null;
        editDescription = "";
        editEntryDate = "";
        editStartTime = "";
        editEndTime = "";
    }

    public boolean isEditing() {
        return editingEntryId != null;
    }

    // accepts "9:00" as well as "09:00", LocalTime.parse only accepts the latter
    private LocalTime parseTime(String value) {
        String trimmed = value.trim();
        try {
            return LocalTime.parse(trimmed);
        } catch (DateTimeParseException e) {
            return LocalTime.parse(trimmed, DateTimeFormatter.ofPattern("H:mm", Locale.ROOT));
        }
    }

    public void deleteEntry(Long id) {
        try {
            service.deleteEntry(id);
            refreshTimesheets();
        } catch (EJBAccessException e) {
            error(UiMessages.get("timesheet.message.ownEntriesOnly"));
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "delete timesheet entry",
                    "common.error.operationFailed", e);
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
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "sign timesheet",
                    "common.error.operationFailed", e);
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
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "revoke timesheet signature",
                    "common.error.operationFailed", e);
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
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "supervisor sign timesheet",
                    "common.error.operationFailed", e);
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
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "request timesheet changes",
                    "common.error.operationFailed", e);
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
        } catch (RuntimeException e) {
            WebExceptionHandler.handle(getClass(), "archive timesheet",
                    "common.error.operationFailed", e);
        }
    }

    public void loadTimesheetForPrinting() {
        try {
            timesheetForPrinting = service.getForPrinting(timesheetDetailId);
        } catch (RuntimeException e) {
            timesheetForPrinting = null;
            WebExceptionHandler.handle(getClass(), "load printable timesheet",
                    "timesheet.message.printUnavailable", e);
        }
    }

    public List<TimesheetDto> getTimesheets() {
        if (timesheets == null) {
            refreshTimesheets();
        }
        return timesheets;
    }

    public List<TimesheetDto> getFilteredTimesheets() {
        List<TimesheetDto> all = getTimesheets();
        if (all == null || RecordSearch.isBlank(searchTerm)) {
            return all;
        }
        return all.stream()
                .filter(t -> RecordSearch.matches(searchTerm, timesheetSearchValues(t)))
                .toList();
    }

    public String employeeLabel(TimesheetDto timesheet) {
        PersonDto employee = employeeOf(timesheet);
        if (employee == null) {
            ContractDto contract = getContractsById().get(timesheet.getContractId());
            return contract == null || contract.getEmployeeName() == null ? "-" : contract.getEmployeeName();
        }
        String[] values = RecordSearch.personValues(employee);
        return values[1].isEmpty() ? values[0] : values[1] + " (" + values[0] + ")";
    }

    private String[] timesheetSearchValues(TimesheetDto timesheet) {
        ContractDto contract = getContractsById().get(timesheet.getContractId());
        List<String> values = new ArrayList<>(List.of(RecordSearch.personValues(employeeOf(timesheet))));
        if (contract != null) {
            values.add(contract.getEmployeeName());
            values.add(contract.getName());
        }
        if (timesheet.getStatus() != null) {
            values.add(statusLabel(timesheet.getStatus()));
        }
        if (timesheet.getStartDate() != null) {
            values.add(timesheet.getStartDate().toString());
            values.add(timesheet.getStartDate().format(SEARCH_DATE_FORMAT));
        }
        if (timesheet.getEndDate() != null) {
            values.add(timesheet.getEndDate().toString());
            values.add(timesheet.getEndDate().format(SEARCH_DATE_FORMAT));
        }
        return values.toArray(String[]::new);
    }

    private PersonDto employeeOf(TimesheetDto timesheet) {
        ContractDto contract = getContractsById().get(timesheet.getContractId());
        return contract == null ? null : getPeopleById().get(contract.getEmployeeId());
    }

    private Map<Long, ContractDto> getContractsById() {
        if (contractsById == null) {
            contractsById = new HashMap<>();
            try {
                for (ContractDto contract : contractService.findAll()) {
                    contractsById.put(contract.getId(), contract);
                }
            } catch (RuntimeException exception) {
                WebExceptionHandler.handle(getClass(), "load contracts for timesheet search",
                        "common.error.loadFailed", exception);
            }
        }
        return contractsById;
    }

    private Map<Long, PersonDto> getPeopleById() {
        if (peopleById == null) {
            peopleById = new HashMap<>();
            if (!RecordSearch.canListPeople()) {
                return peopleById;
            }
            try {
                for (PersonDto person : personService.findAll()) {
                    peopleById.put(person.getId(), person);
                }
            } catch (RuntimeException exception) {
                WebExceptionHandler.handle(getClass(), "load people for timesheet search",
                        "common.error.loadFailed", exception);
            }
        }
        return peopleById;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public List<TimesheetDto> getEntryManageableTimesheets() {
        if (entryManageableTimesheets == null) {
            entryManageableTimesheets = safelyLoad(
                    service::findForEntryManagement, "load manageable timesheets");
        }
        return entryManageableTimesheets;
    }

    public List<TimesheetDto> getEmployeeSignatureRevocableTimesheets() {
        if (employeeSignatureRevocableTimesheets == null) {
            employeeSignatureRevocableTimesheets = safelyLoad(
                    service::findForEmployeeSignatureRevocation, "load revocable timesheets");
        }
        return employeeSignatureRevocableTimesheets;
    }

    public List<TimesheetDto> getSupervisorSignableTimesheets() {
        if (supervisorSignableTimesheets == null) {
            supervisorSignableTimesheets = safelyLoad(
                    service::findForSupervisorSigning, "load supervisor-signable timesheets");
        }
        return supervisorSignableTimesheets;
    }

    public List<TimesheetDto> getChangeRequestableTimesheets() {
        if (changeRequestableTimesheets == null) {
            changeRequestableTimesheets = safelyLoad(
                    service::findForChangeRequest, "load change-requestable timesheets");
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
        timesheets = safelyLoad(service::findAll, "load timesheets");
        entryManageableTimesheets = null;
        employeeSignatureRevocableTimesheets = null;
        supervisorSignableTimesheets = null;
        changeRequestableTimesheets = null;
    }

    private List<TimesheetDto> safelyLoad(
            java.util.function.Supplier<List<TimesheetDto>> loader, String operation) {
        try {
            return loader.get();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), operation,
                    "common.error.loadFailed", exception);
            return List.of();
        }
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

    public ReportType getEditEntryType() {
        return editEntryType;
    }

    public void setEditEntryType(ReportType editEntryType) {
        this.editEntryType = editEntryType;
    }

    public String getEditEntryDate() {
        return editEntryDate;
    }

    public void setEditEntryDate(String editEntryDate) {
        this.editEntryDate = editEntryDate;
    }

    public String getEditStartTime() {
        return editStartTime;
    }

    public void setEditStartTime(String editStartTime) {
        this.editStartTime = editStartTime;
    }

    public String getEditEndTime() {
        return editEndTime;
    }

    public void setEditEndTime(String editEndTime) {
        this.editEndTime = editEndTime;
    }

    public String getEditDescription() {
        return editDescription;
    }

    public void setEditDescription(String editDescription) {
        this.editDescription = editDescription;
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
