package sierra.tms.web;

import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;
import sierra.tms.dto.ContractDto;
import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.services.ContractService;
import sierra.tms.services.FeatureAccessService;
import sierra.tms.services.TimesheetService;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.ReportType;
import sierra.tms.utils.enums.TimeSheetStatus;

@Named
@ViewScoped
public class StatisticsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ContractService contractService;

    @EJB
    private TimesheetService timesheetService;

    @EJB
    private FeatureAccessService featureAccessService;

    private List<ContractSummary> summaries;

    public void load() {
        try {
            summaries = new ArrayList<>();
            for (ContractDto contract : contractService.findAll()) {
                summaries.add(new ContractSummary(contract,
                        timesheetService.findByContractId(contract.getId())));
            }
        } catch (EJBException exception) {
            summaries = null;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "The statistics could not be loaded.", null));
        }
    }

    public List<ContractSummary> getSummaries() {
        return summaries;
    }

    public boolean isEmployee() {
        return featureAccessService.isReportWork();
    }

    public boolean isSupervisor() {
        return featureAccessService.isCountersignTimesheet();
    }

    public double getHoursDue() {
        return sum(ContractSummary::getHoursDue);
    }

    public double getHoursReported() {
        return sum(ContractSummary::getHoursReported);
    }

    public double getBalance() {
        return sum(ContractSummary::getBalance);
    }

    public double getVacationUsed() {
        return sum(ContractSummary::getVacationUsed);
    }

    public double getVacationAllowed() {
        return sum(ContractSummary::getVacationAllowed);
    }

    public int getOverdueCount() {
        return (int) sum(s -> s.getOverdue().size());
    }

    public int getAwaitingSignatureCount() {
        return (int) sum(s -> s.getAwaitingSignature().size());
    }

    private double sum(ToDoubleFunction<ContractSummary> value) {
        return summaries == null ? 0 : summaries.stream().mapToDouble(value).sum();
    }

    public String statusLabel(ContractStatus status) {
        return switch (status) {
            case PREPARED -> "Prepared";
            case STARTED -> "Started";
            case TERMINATED -> "Terminated";
            case ARCHIVED -> "Archived";
        };
    }

    public String statusStyleClass(ContractStatus status) {
        return "contract-statistics__status--" + status.name().toLowerCase();
    }

    public static class ContractSummary implements Serializable {

        private static final long serialVersionUID = 1L;

        private final ContractDto contract;
        private final List<TimesheetDto> timesheets;
        private final double vacationUsed;
        private final List<TimesheetDto> overdue = new ArrayList<>();
        private final List<TimesheetDto> awaitingSignature = new ArrayList<>();
        private int inProgress;
        private int signedByEmployee;
        private int signedBySupervisor;
        private int archived;

        ContractSummary(ContractDto contract, List<TimesheetDto> timesheets) {
            this.contract = contract;
            this.timesheets = timesheets;

            LocalDate today = LocalDate.now();
            double vacation = 0;

            for (TimesheetDto timesheet : timesheets) {
                vacation += vacationHoursOf(timesheet);
                countStatus(timesheet);

                if (timesheet.getStatus() == TimeSheetStatus.IN_PROGRESS
                        && timesheet.getEndDate().isBefore(today)) {
                    overdue.add(timesheet);
                }
                if (timesheet.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
                    awaitingSignature.add(timesheet);
                }
            }
            this.vacationUsed = vacation;
        }

        private static double vacationHoursOf(TimesheetDto timesheet) {
            if (timesheet.getEntries() == null) {
                return 0;
            }
            return timesheet.getEntries().stream()
                    .filter(e -> e.getType() == ReportType.VACATION)
                    .mapToDouble(TimesheetEntryDto::getHours)
                    .sum();
        }

        private void countStatus(TimesheetDto timesheet) {
            switch (timesheet.getStatus()) {
                case IN_PROGRESS -> inProgress++;
                case SIGNED_BY_EMPLOYEE -> signedByEmployee++;
                case SIGNED_BY_SUPERVISOR -> signedBySupervisor++;
                case ARCHIVED -> archived++;
            }
        }

        public ContractDto getContract() {
            return contract;
        }

        public int getTimesheetCount() {
            return timesheets.size();
        }

        public double getHoursDue() {
            return contract.getTotalHoursDue();
        }

        public double getBalance() {
            return contract.getRemainingHours();
        }

        public double getHoursReported() {
            return getHoursDue() - getBalance();
        }

        public double getVacationUsed() {
            return vacationUsed;
        }

        public double getVacationAllowed() {
            return contract.getVacationHours();
        }

        public int getHoursPercent() {
            return percent(getHoursReported(), getHoursDue());
        }

        public int getVacationPercent() {
            return percent(vacationUsed, getVacationAllowed());
        }

        public int getInProgressPercent() {
            return percent(inProgress, timesheets.size());
        }

        public int getSignedByEmployeePercent() {
            return percent(signedByEmployee, timesheets.size());
        }

        public int getSignedBySupervisorPercent() {
            return percent(signedBySupervisor, timesheets.size());
        }

        public int getArchivedPercent() {
            return percent(archived, timesheets.size());
        }

        private static int percent(double part, double whole) {
            return whole <= 0 ? 0 : (int) Math.round(part / whole * 100);
        }

        public int getInProgress() {
            return inProgress;
        }

        public int getSignedByEmployee() {
            return signedByEmployee;
        }

        public int getSignedBySupervisor() {
            return signedBySupervisor;
        }

        public int getArchived() {
            return archived;
        }

        public List<TimesheetDto> getOverdue() {
            return overdue;
        }

        public List<TimesheetDto> getAwaitingSignature() {
            return awaitingSignature;
        }
    }
}
