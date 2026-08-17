package sierra.tms.services;

import jakarta.ejb.Local;
import java.time.LocalDate;
import sierra.tms.entities.ContractEntity;

@Local
public interface ContractHoursCalculationService {
    
    double calculateVacationHours(ContractEntity contract);
    
    double calculateHoursDue(ContractEntity contract, LocalDate periodStart, LocalDate periodEnd);
    
    double calculateTimesheetHoursDue(Long timesheetId);
    
    double calculateTotalHoursDueForContract(Long contractId);
    
    double calculateTotalReportedHoursForContract(Long contractId);
    
    double calculateRemainingHours(double totalHoursDue, double totalReportedHours);
    
}
