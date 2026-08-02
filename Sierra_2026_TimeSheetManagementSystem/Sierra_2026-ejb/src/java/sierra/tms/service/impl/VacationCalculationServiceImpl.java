package sierra.tms.service.impl;

import sierra.tms.service.VacationCalculationService;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityNotFoundException;
import java.time.temporal.ChronoUnit;
import sierra.tms.dao.ContractDao;
import sierra.tms.entities.ContractEntity;

/**
 *
 * @author prajnashetty
 */
@Stateless
public class VacationCalculationServiceImpl
        implements VacationCalculationService {

    @EJB
    private ContractDao contractDao;

    @Override
    public double calculateVacationHours(Long contractId) {

        ContractEntity contract = contractDao.findById(contractId);

        if (contract == null) {
            throw new EntityNotFoundException(
                    "Contract not found.");
        }

        return calculateVacationHours(contract);
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
}
