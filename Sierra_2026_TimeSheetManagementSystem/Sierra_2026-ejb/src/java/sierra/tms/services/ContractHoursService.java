package sierra.tms.services;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import sierra.tms.entities.ContractEntity;

@Stateless
public class ContractHoursService {

    @EJB
    private HolidayService holidayService;

    public BigDecimal calculateVacationHours(ContractEntity contract) {
        validateContract(contract);

        long durationInMonths = ChronoUnit.MONTHS.between(
                YearMonth.from(contract.getStartDate()),
                YearMonth.from(contract.getEndDate())
        ) + 1;

        return BigDecimal.valueOf(contract.getVacationDaysPerYear())
                .multiply(BigDecimal.valueOf(durationInMonths))
                .divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(contract.getHoursPerWeek()))
                .divide(BigDecimal.valueOf(contract.getWorkingDaysPerWeek()),
                        2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTimesheetHoursDue(
            ContractEntity contract,
            LocalDate periodStart,
            LocalDate periodEnd) {

        validateContract(contract);

        if (periodStart == null || periodEnd == null
                || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Invalid timesheet period.");
        }

        int workingDays = 0;
        int publicHolidays = 0;

        for (LocalDate day = periodStart;
                !day.isAfter(periodEnd);
                day = day.plusDays(1)) {

            if (isWorkingDay(day, contract.getWorkingDaysPerWeek())) {
                workingDays++;

                if (holidayService.isPublicHoliday(day)) {
                    publicHolidays++;
                }
            }
        }

        return BigDecimal.valueOf(workingDays - publicHolidays)
                .multiply(BigDecimal.valueOf(contract.getHoursPerWeek()))
                .divide(BigDecimal.valueOf(contract.getWorkingDaysPerWeek()),
                        2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRemainingHours(
            BigDecimal totalHoursDue,
            BigDecimal totalReportedHours) {

        return totalHoursDue.subtract(totalReportedHours);
    }

    private boolean isWorkingDay(LocalDate day, int workingDaysPerWeek) {
        return day.getDayOfWeek().getValue() <= workingDaysPerWeek;
    }

    private void validateContract(ContractEntity contract) {
        if (contract == null
                || contract.getStartDate() == null
                || contract.getEndDate() == null
                || contract.getHoursPerWeek() == null
                || contract.getWorkingDaysPerWeek() == null
                || contract.getWorkingDaysPerWeek() < 1
                || contract.getWorkingDaysPerWeek() > 7) {
            throw new IllegalArgumentException("Contract data is incomplete.");
        }
    }
}
