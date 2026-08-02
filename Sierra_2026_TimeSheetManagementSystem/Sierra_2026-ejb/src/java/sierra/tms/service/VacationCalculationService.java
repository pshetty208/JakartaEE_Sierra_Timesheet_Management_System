package sierra.tms.service;

import jakarta.ejb.Remote;

@Remote
public interface VacationCalculationService {
    
    double calculateVacationHours(Long contractId);
    
}
