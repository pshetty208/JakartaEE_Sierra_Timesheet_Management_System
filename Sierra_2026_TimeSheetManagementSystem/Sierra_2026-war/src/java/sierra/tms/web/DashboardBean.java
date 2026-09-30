package sierra.tms.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import sierra.tms.dto.ContractDto;
import sierra.tms.dto.PersonDto;
import sierra.tms.dto.TimesheetDto;
import sierra.tms.services.ContractService;
import sierra.tms.services.PersonService;
import sierra.tms.services.TimesheetService;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.RoleType;
import sierra.tms.utils.enums.TimeSheetStatus;

@Named
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int RECENT_ITEM_LIMIT = 5;
    @EJB
    private TimesheetService timesheetService;

    @EJB
    private ContractService contractService;

    @EJB
    private PersonService personService;

    private RoleType dashboardRole;
    private List<TimesheetDto> timesheets;
    private List<TimesheetDto> pendingTimesheets;
    private List<TimesheetDto> recentlyApprovedTimesheets;
    private List<TimesheetDto> inProgressTimesheets;
    private List<TimesheetDto> submittedTimesheets;
    private List<TimesheetDto> readyToArchiveTimesheets;
    private List<TimesheetDto> archivedTimesheets;
    private List<TimesheetDto> recentTimesheets;
    private List<ContractDto> contracts;
    private Map<Long, ContractDto> contractsById;
    private Map<Long, PersonDto> peopleById;
    private long signedThisMonthCount;

    @PostConstruct
    public void init() {
        dashboardRole = resolveRole();
        contracts = loadContracts();
        contractsById = contracts.stream()
                .collect(Collectors.toMap(ContractDto::getId, Function.identity()));
        peopleById = dashboardRole == RoleType.EMPLOYEE
                ? Map.of()
                : loadPeople().stream()
                        .collect(Collectors.toMap(PersonDto::getId, Function.identity()));
        timesheets = loadTimesheets();

        pendingTimesheets = byStatus(TimeSheetStatus.SIGNED_BY_EMPLOYEE,
                Comparator.comparing(TimesheetDto::getSignedByEmployee,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        submittedTimesheets = pendingTimesheets;
        inProgressTimesheets = byStatus(TimeSheetStatus.IN_PROGRESS,
                Comparator.comparing(TimesheetDto::getEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        readyToArchiveTimesheets = byStatus(TimeSheetStatus.SIGNED_BY_SUPERVISOR,
                Comparator.comparing(TimesheetDto::getSignedBySupervisor,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        archivedTimesheets = byStatus(TimeSheetStatus.ARCHIVED,
                Comparator.comparing(TimesheetDto::getSignedBySupervisor,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        recentlyApprovedTimesheets = timesheets.stream()
                .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.SIGNED_BY_SUPERVISOR
                        || timesheet.getStatus() == TimeSheetStatus.ARCHIVED)
                .sorted(Comparator.comparing(TimesheetDto::getSignedBySupervisor,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_ITEM_LIMIT)
                .toList();
        recentTimesheets = timesheets.stream()
                .sorted(Comparator.comparing(this::activityDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(RECENT_ITEM_LIMIT)
                .toList();

        LocalDate today = LocalDate.now();
        signedThisMonthCount = timesheets.stream()
                .map(TimesheetDto::getSignedBySupervisor)
                .filter(date -> date != null
                        && date.getYear() == today.getYear()
                        && date.getMonth() == today.getMonth())
                .count();
    }

    private RoleType resolveRole() {
        var externalContext = FacesContext.getCurrentInstance().getExternalContext();
        for (RoleType role : RoleType.values()) {
            if (externalContext.isUserInRole(role.name())) {
                return role;
            }
        }
        return RoleType.EMPLOYEE;
    }

    private List<TimesheetDto> byStatus(TimeSheetStatus status,
            Comparator<TimesheetDto> comparator) {
        return timesheets.stream()
                .filter(timesheet -> timesheet.getStatus() == status)
                .sorted(comparator)
                .limit(RECENT_ITEM_LIMIT)
                .toList();
    }

    private LocalDate activityDate(TimesheetDto timesheet) {
        if (timesheet.getSignedBySupervisor() != null) {
            return timesheet.getSignedBySupervisor();
        }
        if (timesheet.getSignedByEmployee() != null) {
            return timesheet.getSignedByEmployee();
        }
        return timesheet.getEndDate();
    }

    private List<TimesheetDto> loadTimesheets() {
        try {
            return timesheetService.findAll();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "load dashboard timesheets",
                    "common.error.loadFailed", exception);
            return List.of();
        }
    }

    private List<ContractDto> loadContracts() {
        try {
            return contractService.findAll();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "load dashboard contracts",
                    "common.error.loadFailed", exception);
            return List.of();
        }
    }

    private List<PersonDto> loadPeople() {
        try {
            return personService.findAll();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "load dashboard people",
                    "common.error.loadFailed", exception);
            return List.of();
        }
    }

    public String getViewPath() {
        return "/WEB-INF/dashboard/"
                + dashboardFileName()
                + ".xhtml";
    }

    /** The dashboard include files are named after the role, except ADMIN, whose file is "administrator.xhtml". */
    private String dashboardFileName() {
        return dashboardRole == RoleType.ADMIN
                ? "administrator"
                : dashboardRole.name().toLowerCase(Locale.ROOT);
    }

    public String getPageTitleKey() {
        return "dashboard."
                + dashboardRole.name().toLowerCase(Locale.ROOT)
                + ".page.title";
    }

    public long getTimesheetCount() {
        return timesheets.size();
    }

    public long getInProgressCount() {
        return countTimesheets(TimeSheetStatus.IN_PROGRESS);
    }

    public long getPendingApprovalCount() {
        return countTimesheets(TimeSheetStatus.SIGNED_BY_EMPLOYEE);
    }

    public long getApprovedCount() {
        return countTimesheets(TimeSheetStatus.SIGNED_BY_SUPERVISOR);
    }

    public long getArchivedCount() {
        return countTimesheets(TimeSheetStatus.ARCHIVED);
    }

    public long getCompletedCount() {
        return getApprovedCount() + getArchivedCount();
    }

    private long countTimesheets(TimeSheetStatus status) {
        return timesheets.stream()
                .filter(timesheet -> timesheet.getStatus() == status)
                .count();
    }

    public long getActiveContractCount() {
        return countContracts(ContractStatus.STARTED);
    }

    public long getPreparedContractCount() {
        return countContracts(ContractStatus.PREPARED);
    }

    public long getTerminatedContractCount() {
        return countContracts(ContractStatus.TERMINATED);
    }

    private long countContracts(ContractStatus status) {
        return contracts.stream()
                .filter(contract -> contract.getStatus() == status)
                .count();
    }

    public long getTeamMemberCount() {
        return contracts.stream()
                .filter(contract -> contract.getStatus() == ContractStatus.STARTED)
                .map(ContractDto::getEmployeeId)
                .distinct()
                .count();
    }

    public int getPeopleCount() {
        return peopleById.size();
    }

    public long getSignedThisMonthCount() {
        return signedThisMonthCount;
    }

    public List<TimesheetDto> getPendingTimesheets() {
        return pendingTimesheets;
    }

    public List<TimesheetDto> getRecentlyApprovedTimesheets() {
        return recentlyApprovedTimesheets;
    }

    public List<TimesheetDto> getInProgressTimesheets() {
        return inProgressTimesheets;
    }

    public List<TimesheetDto> getSubmittedTimesheets() {
        return submittedTimesheets;
    }

    public List<TimesheetDto> getReadyToArchiveTimesheets() {
        return readyToArchiveTimesheets;
    }

    public List<TimesheetDto> getArchivedTimesheets() {
        return archivedTimesheets;
    }

    public List<TimesheetDto> getRecentTimesheets() {
        return recentTimesheets;
    }

    public String employeeName(TimesheetDto timesheet) {
        ContractDto contract = contractsById.get(timesheet.getContractId());
        PersonDto employee = contract == null ? null : peopleById.get(contract.getEmployeeId());
        if (employee == null) {
            return "-";
        }

        String name = ((employee.getFirstName() == null ? "" : employee.getFirstName())
                + " " + (employee.getLastName() == null ? "" : employee.getLastName())).trim();
        return name.isEmpty() ? employee.getEmailAddress() : name;
    }

    public String contractName(TimesheetDto timesheet) {
        ContractDto contract = contractsById.get(timesheet.getContractId());
        return contract == null ? "-" : contract.getName();
    }

    public String statusMessageKey(TimesheetDto timesheet) {
        return switch (timesheet.getStatus()) {
            case IN_PROGRESS -> "dashboard.status.inProgress";
            case SIGNED_BY_EMPLOYEE -> "dashboard.status.submitted";
            case SIGNED_BY_SUPERVISOR -> "dashboard.status.approved";
            case ARCHIVED -> "dashboard.status.archived";
        };
    }

    public String statusStyleClass(TimesheetDto timesheet) {
        return timesheet.getStatus().name().toLowerCase(Locale.ROOT);
    }

    public double reportedHours(TimesheetDto timesheet) {
        return timesheet.getEntries() == null
                ? 0.0
                : timesheet.getEntries().stream().mapToDouble(entry -> entry.getHours()).sum();
    }
}
