package sierra.tms.utils;

import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.utils.enums.TimeSheetStatus;


@Singleton
public class ArchivingService {
    
    private static final Logger LOGGER = Logger.getLogger(ArchivingService.class.getName());

    @EJB
    private TimesheetDao timesheetDao;

    @EJB
    private ContractDao contractDao;

    // Once a day - 01:00.
    @Schedule(hour = "1", minute = "0", persistent = false)
    public void archiveTimesheets() {
        LocalDate today = LocalDate.now();

        for (TimesheetEntity timesheet : timesheetDao.findAll()) {
            if (timesheet.getStatus() != TimeSheetStatus.ARCHIVED
                    || timesheet.getSignedBySupervisor() == null) {
                continue;
            }

            ContractEntity contract = timesheet.getContract();
            int retentionMonths = contract.getArchiveDuration(); //default - 24

            LocalDate expiry = timesheet.getSignedBySupervisor().plusMonths(retentionMonths);
            if (!today.isBefore(expiry)) {
                LOGGER.log(Level.INFO, () -> "Archiving expired timesheet " + timesheet.getId()
                        + " (contract " + contract.getId() + ")");
                timesheetDao.delete(timesheet.getId());
            }
        }

        archiveContracts();
    }


    private void archiveContracts() {
        for (ContractEntity contract : contractDao.findAll()) {
            List<TimesheetEntity> remaining = timesheetDao.findByContractId(contract.getId());
            if (remaining.isEmpty()) {
                LOGGER.log(Level.INFO, () -> "Deleting contract " + contract.getId() +  ", as all timesheets are archived");
                contractDao.delete(contract);
            }
        }
    }
}
    