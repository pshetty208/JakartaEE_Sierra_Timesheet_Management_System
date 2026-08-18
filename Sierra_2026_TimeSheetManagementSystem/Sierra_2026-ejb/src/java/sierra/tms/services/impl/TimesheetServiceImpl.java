package sierra.tms.services.impl;

import jakarta.annotation.Resource;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.dao.TimesheetEntryDao;
import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.entities.TimesheetEntryEntity;
import sierra.tms.services.TimesheetService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import sierra.tms.dao.PersonDao;
import sierra.tms.entities.PersonEntity;
import sierra.tms.services.ContractHoursCalculationService;
import sierra.tms.services.ContractService;
import sierra.tms.utils.enums.ReportType;
import sierra.tms.utils.enums.TimeSheetStatus;

@Stateless
public class TimesheetServiceImpl implements TimesheetService {

    @EJB
    private TimesheetDao timesheetDao;

    @EJB
    private TimesheetEntryDao entryDao;

    @EJB
    private ContractDao contractDao;
    
    @EJB
    private ContractService contractService;
    
    @EJB
    private ContractHoursCalculationService calculationService;

    @EJB
    private PersonDao personDao;
    
    @Resource
    private SessionContext sessionContext;
    
    private static final int DEFAULT_ARCHIVE_DURATION_MONTHS = 24;

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public Long save(TimesheetDto timesheet) {

        ContractEntity contract = contractDao.findById(timesheet.getContractId());

        if (contract == null) {
            return null;
        }

        TimesheetEntity entity = new TimesheetEntity();
        entity.setContract(contract);
        entity.setStartDate(timesheet.getStartDate());
        entity.setEndDate(timesheet.getEndDate());

        if (timesheet.getStatus() != null) {
            entity.setStatus(timesheet.getStatus());
        }

        timesheetDao.save(entity);

        return entity.getId();
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public TimesheetDto findById(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);
        if (entity == null) {
            throw new EntityNotFoundException("Timesheet not found: " + id);
        }
        
        PersonEntity currentPerson = getCurrentPerson();
        if (!isAuthorizedToViewTimesheet(currentPerson, entity)) {
            throw new EJBAccessException(currentPerson + " is not authorized to view this timesheet.");
        }

        return createDTO(entity);
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public List<TimesheetDto> findAll() {
        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findAll()
                .stream()
                .filter(t -> isAuthorizedToViewTimesheet(currentPerson, t))
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public List<TimesheetDto> findByContractId(Long contractId) {
        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findByContractId(contractId)
                .stream()
                .filter(t -> isAuthorizedToViewTimesheet(currentPerson, t))
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void update(TimesheetDto timesheet) {

        TimesheetEntity entity = timesheetDao.findById(timesheet.getId());

        if (entity == null) {
            throw new EntityNotFoundException("Timesheet not found: " + timesheet.getId());
        }
        
        if (entity.getStatus() == TimeSheetStatus.ARCHIVED) {
            throw new IllegalStateException("Archived timesheets cannot be changed.");
        }

        entity.setStartDate(timesheet.getStartDate());
        entity.setEndDate(timesheet.getEndDate());
        entity.setStatus(timesheet.getStatus());
//        entity.setSignedByEmployee(timesheet.getSignedByEmployee());
//        entity.setSignedBySupervisor(timesheet.getSignedBySupervisor());

        timesheetDao.update(entity);
        
//        if (entity.getStatus() == TimeSheetStatus.ARCHIVED && entity.getContract() != null) {
//            contractService.archiveContract(entity.getContract().getId());
//        }
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void delete(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new EntityNotFoundException("Timesheet not found: " + id);
        }
        
        if (entity.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE || entity.getStatus() == TimeSheetStatus.SIGNED_BY_SUPERVISOR
                || entity.getStatus() == TimeSheetStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot delete a timesheet that has been signed by the employee or supervisor.");
        }
        timesheetDao.delete(id);
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public Long addEntry(Long timesheetId, TimesheetEntryDto entry) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        validateTimesheet(timesheet);
        
        double hours = computeHours(entry.getStartTime(), entry.getEndTime());
        if (entry.getType() == ReportType.VACATION) {
            validateVacationHours(null, timesheet, hours);
        }

        TimesheetEntryEntity entity = new TimesheetEntryEntity();
        entity.setType(entry.getType());
        entity.setDescription(entry.getDescription());
        entity.setEntryDate(entry.getEntryDate());
        entity.setStartTime(entry.getStartTime());
        entity.setEndTime(entry.getEndTime());

        timesheet.addEntry(entity);
        entryDao.save(entity);

        return entity.getId();
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void updateEntry(TimesheetEntryDto entry) {
        TimesheetEntryEntity entity = entryDao.findById(entry.getId());

        if (entity == null) {
             throw new EntityNotFoundException("Timesheet Entry with id:" + entry.getId() + " not found");
        }

        TimesheetEntity timesheet = entity.getTimesheet();
        validateTimesheet(timesheet);
        
        double hours = computeHours(entry.getStartTime(), entry.getEndTime());
        if (entry.getType() == ReportType.VACATION) {
            validateVacationHours(entity.getId(), timesheet, hours);
        }
        
        entity.setType(entry.getType());
        entity.setDescription(entry.getDescription());
        entity.setEntryDate(entry.getEntryDate());
        entity.setStartTime(entry.getStartTime());
        entity.setEndTime(entry.getEndTime());

        entryDao.update(entity);
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void deleteEntry(Long entryId) {
        TimesheetEntryEntity entity = entryDao.findById(entryId);
        if (entity == null) {
            throw new EntityNotFoundException("Timesheet Entry with id:" + entryId + " not found");
        }

        validateTimesheet(entity.getTimesheet());
        
        entryDao.delete(entryId);
    }
    
    @Override
    @RolesAllowed({"SECRETARY"})
    public TimesheetDto getForPrinting(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new EntityNotFoundException("Timesheet with id:" + id + " not found");
        }

        return createDTO(entity);
    }
    
    @Override
    @RolesAllowed({"SECRETARY"})
    public void archiveTimesheet(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new EntityNotFoundException("Timesheet not found: " + id);
        }

        if (entity.getStatus() != TimeSheetStatus.SIGNED_BY_SUPERVISOR) {
            throw new IllegalStateException("Only timesheets in SIGNED_BY_SUPERVISOR status can be archived.");
        }

        entity.setStatus(TimeSheetStatus.ARCHIVED);
        timesheetDao.update(entity);

        if (entity.getContract() != null) {
            contractService.archiveContract(entity.getContract().getId());
        }
    }
    
    @Schedule(hour = "2", minute = "0", second = "0", persistent = true)
    public void deleteExpiredTimesheets() {
        List<TimesheetEntity> archived = timesheetDao.findByStatus(TimeSheetStatus.ARCHIVED);
        LocalDate today = LocalDate.now();

        for (TimesheetEntity timesheet : archived) {
            if (timesheet.getSignedBySupervisor() == null) {
                continue;
            }

            ContractEntity contract = timesheet.getContract();
            int archiveDurationMonths = (contract != null && contract.getArchiveDuration() != null) 
                    ? contract.getArchiveDuration() : DEFAULT_ARCHIVE_DURATION_MONTHS;

            LocalDate expiryDate = timesheet.getSignedBySupervisor().plusMonths(archiveDurationMonths);
            if (today.isBefore(expiryDate)) {
                continue;
            }

            Long timesheetId = timesheet.getId();
            timesheetDao.delete(timesheet);

            if (contract == null) {
                continue;
            }
            boolean anyTimesheetsRemain = timesheetDao.findByContractId(contract.getId())
                    .stream()
                    .anyMatch(t -> !t.getId().equals(timesheetId));
            if (!anyTimesheetsRemain) {
                contractDao.delete(contract);
            }
        }
    }
    
    private double computeHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return 0.0;
        }
        return Duration.between(startTime, endTime).toMinutes() / 60.0;
    }
    
    private void validateVacationHours(Long entryId, TimesheetEntity timesheet, double hours) {
        ContractEntity contract = timesheet.getContract();
        double reportedVacationHours = timesheetDao.findByContractId(contract.getId())
                                        .stream()
                                        .flatMap(t -> t.getEntries().stream())
                                        .filter(e -> e.getType() == ReportType.VACATION && (entryId == null || !entryId.equals(e.getId())))
                                        .mapToDouble(TimesheetEntryEntity::getHours)
                                        .sum();
        double vacationLimit = calculationService.calculateVacationHours(contract);

        if (reportedVacationHours + hours > vacationLimit) {
            throw new IllegalStateException("The reported vacation hours: " + (reportedVacationHours + hours)
                                + " exceeds the contract's vacation hours: " + vacationLimit);
        }
    }
    
    private void validateTimesheet(TimesheetEntity timesheet){
        
        if (timesheet == null) {
            throw new EntityNotFoundException("Timesheet not found.");
        }
        
        ContractEntity contract = timesheet.getContract();
        
        if (timesheet.getStatus() != TimeSheetStatus.IN_PROGRESS || contract == null || contract.getStatus() != sierra.tms.utils.enums.ContractStatus.STARTED) {
            throw new IllegalStateException("Timesheet entries can only be changed while the timesheet is IN_PROGRESS state and contract is STARTED state");
        }
    }
    
    private PersonEntity getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return personDao.findByEmailAddress(emailAddress);
    }

    private boolean isAuthorizedToViewTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        if (contract == null || person == null) {
            return false;
        }
        Long personId = person.getId();
        return personId.equals(contract.getEmployee().getId())
                || personId.equals(contract.getSupervisor().getId())
                || containsPerson(contract.getAssistants(), personId)
                || containsPerson(contract.getSecretaries(), personId);
    }

    private boolean isEmployeeOnTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        return contract != null && contract.getEmployee() != null
                && person != null && person.getId().equals(contract.getEmployee().getId());
    }

    private boolean containsPerson(Set<PersonEntity> persons, Long personId) {
        return persons != null && persons.stream()
                .anyMatch(p -> p != null && personId.equals(p.getId()));
    }

    private void validateEmployeeOwnsTimesheet(TimesheetEntity timesheet) {
        PersonEntity currentPerson = getCurrentPerson();
        if (!isEmployeeOnTimesheet(currentPerson, timesheet)) {
            throw new EJBAccessException("Only the employee on this contract may manage its timesheet entries.");
        }
    }

    private TimesheetDto createDTO(TimesheetEntity entity) {
        return new TimesheetDto(
                entity.getId(),
                entity.getContract() == null ? null : entity.getContract().getId(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getSignedByEmployee(),
                entity.getSignedBySupervisor(),
                entity.getEntries()
                        .stream()
                        .map(this::createDTO)
                        .toList()
        );
    }

    private TimesheetEntryDto createDTO(TimesheetEntryEntity entity) {
        return new TimesheetEntryDto(
                entity.getId(),
                entity.getTimesheet() == null ? null : entity.getTimesheet().getId(),
                entity.getType(),
                entity.getDescription(),
                entity.getEntryDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getHours()
        );
    }

}
