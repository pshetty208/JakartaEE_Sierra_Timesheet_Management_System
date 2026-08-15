package sierra.tms.services.impl;

import sierra.tms.services.VacationCalculationService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.services.HolidayService;

@Stateless
public class VacationCalculationServiceImpl
        implements VacationCalculationService {

    @EJB
    private ContractDao contractDao;
    
    @EJB
    private TimesheetDao timesheetDao;
    
    @EJB
    private HolidayService holidayService;

    @Override
    public double calculateVacationHours(Long contractId) {
        ContractEntity contract = contractDao.findById(contractId);
        if (contract == null) {
            throw new EntityNotFoundException(
                    "Contract not found.");
        }
        return calculateVacationHours(contract);
    }

    @Override
    public double calculateHoursDue(LocalDate startDate, LocalDate endDate, Double hoursPerWeek, Integer workingDaysPerWeek) {
        int workingDays = countWorkingDays(startDate, endDate);
        
        if (hoursPerWeek == null || workingDaysPerWeek == null) {
            throw new IllegalArgumentException("Working hours must not be null.");
        }

        if (workingDaysPerWeek <= 0) {
            throw new IllegalArgumentException("Working days per week must be greater than zero.");
        }

        double hoursPerDay = hoursPerWeek / workingDaysPerWeek;

        return workingDays * hoursPerDay;    
    }
    
    @Override
    public double calculateTimesheetHoursDue(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);

        if (timesheet == null) {
            throw new EntityNotFoundException("Timesheet not found.");
        }

        ContractEntity contract = timesheet.getContract();

        return calculateHoursDue(
            timesheet.getStartDate(),
            timesheet.getEndDate(),
            (double) contract.getHoursPerWeek(),
            contract.getWorkingDaysPerWeek());
    }
    
    private double calculateVacationHours(ContractEntity contract) {

        long months =
                ChronoUnit.MONTHS.between(
                        contract.getStartDate(),
                        contract.getEndDate()) + 1;

        return contract.getVacationDaysPerYear()
                * months
                / 12.0
                * contract.getHoursPerWeek()
                / contract.getWorkingDaysPerWeek();
    }
        
    
    private int countWorkingDays(LocalDate startDate,
                             LocalDate endDate) {
        int workingDays = 0;
        
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Dates must not be null.");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must not be before start date.");
        }

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (isWorkingDay(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    private boolean isWorkingDay(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> false;
            default -> !holidayService.isPublicHoliday(date);
        };
    }
        
}
