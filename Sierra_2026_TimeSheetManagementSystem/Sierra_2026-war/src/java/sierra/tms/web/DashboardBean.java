package sierra.tms.web;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
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

    private List<TimesheetDto> pendingTimesheets;
    private List<TimesheetDto> recentlyApprovedTimesheets;
    private List<ContractDto> contracts;
    private Map<Long, ContractDto> contractsById;
    private Map<Long, PersonDto> peopleById;
    private long pendingApprovalCount;
    private long signedThisMonthCount;

    @PostConstruct
    public void init() {
        contracts = loadContracts();
        contractsById = contracts.stream()
                .collect(Collectors.toMap(ContractDto::getId, Function.identity()));
        peopleById = personService.findAll().stream()
                .collect(Collectors.toMap(PersonDto::getId, Function.identity()));

        List<TimesheetDto> timesheets = loadTimesheets();
        List<TimesheetDto> allPendingTimesheets = timesheets.stream()
                .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE)
                .sorted(Comparator.comparing(TimesheetDto::getSignedByEmployee,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        pendingApprovalCount = allPendingTimesheets.size();
        pendingTimesheets = allPendingTimesheets.stream()
                .limit(RECENT_ITEM_LIMIT)
                .toList();
        recentlyApprovedTimesheets = timesheets.stream()
                .filter(timesheet -> timesheet.getStatus() == TimeSheetStatus.SIGNED_BY_SUPERVISOR
                        || timesheet.getStatus() == TimeSheetStatus.ARCHIVED)
                .sorted(Comparator.comparing(TimesheetDto::getSignedBySupervisor,
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

    private List<TimesheetDto> loadTimesheets() {
        try {
            return timesheetService.findAll();
        } catch (EJBException exception) {
            return List.of();
        }
    }

    private List<ContractDto> loadContracts() {
        try {
            return contractService.findAll();
        } catch (EJBException exception) {
            return List.of();
        }
    }

    public long getActiveContractCount() {
        return contracts.stream()
                .filter(contract -> contract.getStatus() == ContractStatus.STARTED)
                .count();
    }

    public long getTeamMemberCount() {
        return contracts.stream()
                .filter(contract -> contract.getStatus() == ContractStatus.STARTED)
                .map(ContractDto::getEmployeeId)
                .distinct()
                .count();
    }

    public long getSignedThisMonthCount() {
        return signedThisMonthCount;
    }

    public long getPendingApprovalCount() {
        return pendingApprovalCount;
    }

    public List<TimesheetDto> getPendingTimesheets() {
        return pendingTimesheets;
    }

    public List<TimesheetDto> getRecentlyApprovedTimesheets() {
        return recentlyApprovedTimesheets;
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

    public double reportedHours(TimesheetDto timesheet) {
        return timesheet.getEntries() == null
                ? 0.0
                : timesheet.getEntries().stream().mapToDouble(entry -> entry.getHours()).sum();
    }
}
