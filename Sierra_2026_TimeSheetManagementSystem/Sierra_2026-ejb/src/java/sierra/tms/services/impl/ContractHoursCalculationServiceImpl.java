package sierra.tms.services.impl;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.annotation.security.RolesAllowed;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.entities.TimesheetEntryEntity;
import sierra.tms.services.HolidayService;
import sierra.tms.services.ContractHoursCalculationService;

@Stateless
@RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
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
            throw new IllegalArgumentException("The timesheet period is invalid.");
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
            throw new IllegalArgumentException("Timesheet id is required.");
        }
        
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId); 
        if (timesheet == null) {
            throw new EntityNotFoundException("Timesheet not found: " + timesheetId);
        }

        ContractEntity contract = timesheet.getContract();
        return calculateHoursDue(contract, timesheet.getStartDate(), timesheet.getEndDate());
    }
    
    @Override
    public double calculateTotalHoursDueForContract(Long contractId) {
        if (contractId == null) {
            throw new IllegalArgumentException("Contract id is required.");
        }
        ContractEntity contract = contractDao.findById(contractId);
        if (contract == null) {
            throw new EntityNotFoundException("Contract not found: " + contractId);
        }
        return timesheetDao.findByContractId(contractId)
                .stream()
                .mapToDouble(timesheet -> calculateHoursDue(contract, timesheet.getStartDate(), timesheet.getEndDate()))
                .sum();
    }
    
    @Override
    public double calculateTotalReportedHoursForContract(Long contractId) {
        ContractEntity contract = contractDao.findById(contractId);

        if (contract == null) {
            throw new IllegalArgumentException("Contract id is required.");
        }

        return timesheetDao.findByContractId(contractId)
                .stream()
                .flatMap(timesheet -> timesheet.getEntries().stream())
                .mapToDouble(TimesheetEntryEntity::getHours)
                .sum();
    }
    
    @Override
    public double calculateRemainingHours(double totalHoursDue, double totalReportedHours) {
        return totalHoursDue - totalReportedHours;
    }
            
    private void validateContract(ContractEntity contract) {
        if (contract == null || contract.getStartDate() == null
                || contract.getEndDate() == null
                || contract.getHoursPerWeek() == null
                || contract.getWorkingDaysPerWeek() == null) {
            throw new IllegalArgumentException("Contract data is required.");

        }
        
        if (contract.getWorkingDaysPerWeek() < 1 || contract.getWorkingDaysPerWeek() > 5) {
            throw new IllegalArgumentException("Working days per week must be between 1 and 5.");

        }
    }

    private boolean isWorkingDay(LocalDate day, int workingDaysPerWeek) {
        return day.getDayOfWeek().getValue() <= workingDaysPerWeek;
    }

}
