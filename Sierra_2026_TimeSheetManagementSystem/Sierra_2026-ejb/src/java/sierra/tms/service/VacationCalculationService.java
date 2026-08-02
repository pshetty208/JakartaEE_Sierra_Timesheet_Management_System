package sierra.tms.service;

import jakarta.ejb.Remote;
import java.time.LocalDate;

@Remote
public interface VacationCalculationService {
    
    double calculateVacationHours(Long contractId);
    
    double calculateHoursDue(LocalDate startDate,
                         LocalDate endDate,
                         Double hoursPerWeek,
                         Integer workingDaysPerWeek);
    
    double calculateTimesheetHoursDue(Long timesheetId);
    
}
