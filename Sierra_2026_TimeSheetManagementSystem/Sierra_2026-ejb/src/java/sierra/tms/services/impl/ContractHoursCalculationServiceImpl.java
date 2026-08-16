package sierra.tms.services.impl;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.services.HolidayService;
import sierra.tms.services.ContractHoursCalculationService;

@Stateless
public class ContractHoursCalculationServiceImpl implements ContractHoursCalculationService {

    @EJB
    private ContractDao contractDao;
    
    @EJB
    private TimesheetDao timesheetDao;
    
    @EJB
    private HolidayService holidayService;
    
    @Override
    public double calculateVacationHours(ContractEntity contract) {
        validateContract(contract);
        long durationInMonths = ChronoUnit.MONTHS.between(
                YearMonth.from(contract.getStartDate()),
                YearMonth.from(contract.getEndDate())) + 1;

        return contract.getVacationDaysPerYear() * durationInMonths / 12.0 * contract.getHoursPerWeek() / contract.getWorkingDaysPerWeek();
    }
    
    @Override
    public double calculateHoursDue(ContractEntity contract, LocalDate periodStart, LocalDate periodEnd) {
        validateContract(contract);
        int workingDays = 0;
        int publicHolidays = 0;
        
        if (periodStart == null || periodEnd == null || periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Invalid timesheet period.");
        }

        for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
            if (!isWorkingDay(date, contract.getWorkingDaysPerWeek())) {
                continue;
            }
            workingDays++;
            if (holidayService.isPublicHoliday(date)) {
                publicHolidays++;
            }
        }

        int payableDays = workingDays - publicHolidays;
        return (double) payableDays * contract.getHoursPerWeek() / contract.getWorkingDaysPerWeek();
    }
    
        
    @Override
    public double calculateTimesheetHoursDue(Long timesheetId) {
        if (timesheetId == null) {
            throw new EntityNotFoundException("Timesheet not found.");
        }
        
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId); 
        if (timesheet == null) {
            throw new EntityNotFoundException("Timesheet with id: " + timesheetId + " not found.");
        }

        ContractEntity contract = timesheet.getContract();
        return calculateHoursDue(contract, timesheet.getStartDate(), timesheet.getEndDate());
    }
    
    @Override
    public double calculateTotalHoursDueForContract(Long contractId) {
        if (contractId == null) {
            throw new EntityNotFoundException("Contract not found.");
        }
        ContractEntity contract = contractDao.findById(contractId);
        if (contract == null) {
            throw new EntityNotFoundException("Contract with id: " + contractId + " not found.");
        }
        return timesheetDao.findByContract(contractId)
                .stream()
                .mapToDouble(timesheet -> calculateHoursDue(contract, timesheet.getStartDate(), timesheet.getEndDate()))
                .sum();
    }
    
    @Override
    public double calculateRemainingHours(BigDecimal totalHoursDue, BigDecimal totalReportedHours) {
        if (totalHoursDue == null || totalReportedHours == null) {
            throw new IllegalArgumentException("Hours must not be null.");
        }

        return totalHoursDue.subtract(totalReportedHours).doubleValue();
    }
            
    private void validateContract(ContractEntity contract) {
        if (contract == null || contract.getStartDate() == null
                || contract.getEndDate() == null
                || contract.getHoursPerWeek() == null
                || contract.getWorkingDaysPerWeek() == null
                || contract.getWorkingDaysPerWeek() < 1
                || contract.getWorkingDaysPerWeek() > 7) {
            throw new IllegalArgumentException("Contract data is incomplete.");
        }
    }

    private boolean isWorkingDay(LocalDate day, int workingDaysPerWeek) {
        return day.getDayOfWeek().getValue() <= workingDaysPerWeek;
    }

}
