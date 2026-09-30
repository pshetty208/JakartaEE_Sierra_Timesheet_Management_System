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

    @EJB
    private ConfigService configService;

    // Once a day - 01:00.
    @Schedule(hour = "1", minute = "0", persistent = false)
    public void archiveTimesheets() {
        LOGGER.log(Level.INFO, "Archive-retention job started");
        int deletedTimesheets = 0;
        try {
            LocalDate today = LocalDate.now(configService.getTimeZone());

            for (TimesheetEntity timesheet : timesheetDao.findAll()) {
                if (timesheet.getStatus() != TimeSheetStatus.ARCHIVED
                        || timesheet.getSignedBySupervisor() == null) {
                    continue;
                }

                ContractEntity contract = timesheet.getContract();
                int retentionMonths = contract.getArchiveDuration() != null
                        ? contract.getArchiveDuration()
                        : configService.getDefaultArchiveDurationMonths();

                LocalDate expiry = timesheet.getSignedBySupervisor().plusMonths(retentionMonths);
                if (!today.isBefore(expiry)) {
                    LOGGER.log(Level.INFO, () -> "Deleting expired timesheet " + timesheet.getId()
                            + " (contract " + contract.getId() + ")");
                    timesheetDao.delete(timesheet.getId());
                    deletedTimesheets++;
                }
            }

            int deletedContracts = archiveContracts();
            LOGGER.log(Level.INFO,
                    "Archive-retention job completed: timesheets_deleted={0}, contracts_deleted={1}",
                    new Object[]{deletedTimesheets, deletedContracts});
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE,
                    "Archive-retention job failed: timesheets_deleted=" + deletedTimesheets,
                    exception);
            throw exception;
        }
    }


    private int archiveContracts() {
        int deletedContracts = 0;
        for (ContractEntity contract : contractDao.findAll()) {
            List<TimesheetEntity> remaining = timesheetDao.findByContractId(contract.getId());
            if (remaining.isEmpty()) {
                LOGGER.log(Level.INFO, () -> "Deleting contract " + contract.getId() +  ", as all timesheets are archived");
                contractDao.delete(contract);
                deletedContracts++;
            }
        }
        return deletedContracts;
    }
}
