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
import sierra.tms.exceptions.TimesheetEntryOverlapException;
import sierra.tms.services.TimesheetService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.Schedule;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.dao.PersonDao;
import sierra.tms.entities.PersonEntity;
import sierra.tms.exceptions.AccessDeniedException;
import sierra.tms.exceptions.BusinessRuleViolationException;
import sierra.tms.exceptions.InvalidStateTransitionException;
import sierra.tms.exceptions.ResourceNotFoundException;
import sierra.tms.services.ContractHoursCalculationService;
import sierra.tms.services.ContractService;
import sierra.tms.utils.ConfigService;
import sierra.tms.utils.enums.ReportType;
import sierra.tms.utils.enums.TimeSheetStatus;
import sierra.tms.utils.enums.ContractStatus;

@Stateless
public class TimesheetServiceImpl implements TimesheetService {
    
    private static final Logger LOGGER = Logger.getLogger(TimesheetServiceImpl.class.getName());

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
    
    @EJB
    private ConfigService configService;
    
    @Resource
    private SessionContext sessionContext;
    
// Check Usage - before deletion
//    @Override
//    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
//    public Long save(TimesheetDto timesheet) {
//
//        ContractEntity contract = contractDao.findById(timesheet.getContractId());
//
//        if (contract == null) {
//            return null;
//        }
//
//        TimesheetEntity entity = new TimesheetEntity();
//        entity.setContract(contract);
//        entity.setStartDate(timesheet.getStartDate());
//        entity.setEndDate(timesheet.getEndDate());
//
//        if (timesheet.getStatus() != null) {
//            entity.setStatus(timesheet.getStatus());
//        }
//
//        timesheetDao.save(entity);
//
//        return entity.getId();
//    }


    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public TimesheetDto findById(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Timesheet", id);
        }
        
        PersonEntity currentPerson = getCurrentPerson();
        if (!isAuthorizedToViewTimesheet(currentPerson, entity)) {
            throw new AccessDeniedException();
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
            throw new ResourceNotFoundException("Timesheet", timesheet.getId());
        }
        
        if (entity.getStatus() == TimeSheetStatus.ARCHIVED) {
            throw new InvalidStateTransitionException("error.timesheetNotEditable");
        }

        entity.setStartDate(timesheet.getStartDate());
        entity.setEndDate(timesheet.getEndDate());
//        entity.setStatus(timesheet.getStatus());
//        entity.setSignedByEmployee(timesheet.getSignedByEmployee());
//        entity.setSignedBySupervisor(timesheet.getSignedBySupervisor());

        timesheetDao.update(entity);
        LOGGER.log(Level.INFO, "Updated timesheet: timesheet_id=" + timesheet.getId());
        
//        if (entity.getStatus() == TimeSheetStatus.ARCHIVED && entity.getContract() != null) {
//            contractService.archiveContract(entity.getContract().getId());
//        }
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public List<TimesheetDto> findForEntryManagement() {
        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findAll()
                .stream()
                .filter(t -> isEmployeeOnTimesheet(currentPerson, t))
                .filter(t -> t.getStatus() == TimeSheetStatus.IN_PROGRESS
                        && t.getContract().getStatus() == ContractStatus.STARTED)
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void delete(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new ResourceNotFoundException("Timesheet", id);
        }
        
        if (entity.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE || entity.getStatus() == TimeSheetStatus.SIGNED_BY_SUPERVISOR
                || entity.getStatus() == TimeSheetStatus.ARCHIVED) {
            throw new InvalidStateTransitionException("error.timesheetNotEditable");
        }
        timesheetDao.delete(id);
        LOGGER.log(Level.WARNING, "Timesheet deleted: timesheet_id=" + id);

    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public Long addEntry(Long timesheetId, TimesheetEntryDto entry) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        validateTimesheet(timesheet);
        validateEmployeeOwnsTimesheet(timesheet);
        validateNoOverlappingEntry(null, timesheet, entry);

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
        LOGGER.log(Level.INFO, "Added new timesheet Entry: timesheet_id=" + timesheetId);
        return entity.getId();
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void updateEntry(TimesheetEntryDto entry) {
        TimesheetEntryEntity entity = entryDao.findById(entry.getId());

        if (entity == null) {
            throw new ResourceNotFoundException("TimesheetEntry", entry.getId());
        }

        TimesheetEntity timesheet = entity.getTimesheet();
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);
        validateNoOverlappingEntry(entity.getId(), timesheet, entry);
        
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
        LOGGER.log(Level.INFO, "Timesheet entry updated: entry_id=" + entry.getId() + ", timesheet_id=" + timesheet.getId());
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void deleteEntry(Long entryId) {
        TimesheetEntryEntity entity = entryDao.findById(entryId);
        if (entity == null) {
            throw new ResourceNotFoundException("TimesheetEntry", entryId);
        }
        
        Long timesheetId = entity.getTimesheet().getId();
        TimesheetEntity timesheet = entity.getTimesheet();
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);

        timesheet.removeEntry(entity);
        entryDao.delete(entryId);
        LOGGER.log(Level.WARNING, "Timesheet entry deleted: entry_id=" + entryId + ", timesheet_id=" + timesheetId);
    }
    
    @Override
    @RolesAllowed({"SECRETARY"})
    public TimesheetDto getForPrinting(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new ResourceNotFoundException("Timesheet", id);
        }

        PersonEntity currentPerson = getCurrentPerson();
        ContractEntity contract = entity.getContract();
        if (contract == null || currentPerson == null
                || !containsPerson(contract.getSecretaries(), currentPerson.getId())) {
            throw new EJBAccessException("Only a secretary assigned to this contract may print its timesheet.");
        }

        return createDTO(entity);
    }
    
    @Override
    @RolesAllowed({"SECRETARY"})
    public void archiveTimesheet(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new ResourceNotFoundException("Timesheet", id);
        }

        if (entity.getStatus() != TimeSheetStatus.SIGNED_BY_SUPERVISOR) {
            throw new InvalidStateTransitionException("error.timesheetInvalidTransition", entity.getStatus());
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
        int contractsDeleted = 0;
        int timesheetsDeleted = 0;

        for (TimesheetEntity timesheet : archived) {
            try {
                if (timesheet.getSignedBySupervisor() == null) {
                    continue;
                }

                ContractEntity contract = timesheet.getContract();
                int archiveDurationMonths;
                
                if (contract != null && contract.getArchiveDuration() != null) {
                    archiveDurationMonths = contract.getArchiveDuration();
                } else {
                    archiveDurationMonths = configService.getDefaultArchiveDurationMonths();
                }

                LocalDate expiryDate = timesheet.getSignedBySupervisor().plusMonths(archiveDurationMonths);
                if (today.isBefore(expiryDate)) {
                    continue;
                }

                Long timesheetId = timesheet.getId();
                timesheetDao.delete(timesheet);
                timesheetsDeleted++;
                LOGGER.log(Level.INFO, "Deleted expired timesheet : timesheet_id=" + timesheetId + ", signed by supervisor_id=" + timesheet.getSignedBySupervisor() + ", archive duration=" + archiveDurationMonths); 

                if (contract == null) {
                    continue;
                }
                
                boolean anyTimesheetsRemain = timesheetDao.findByContractId(contract.getId())
                        .stream()
                        .anyMatch(t -> !t.getId().equals(timesheetId));
                
                if (!anyTimesheetsRemain) {
                    Long contractId = contract.getId();
                    contractDao.delete(contract);
                    contractsDeleted++;
                    
                    LOGGER.log(Level.INFO, "Deleted contract: contract_id=" + contractId + ", because no timesheets are remaining");
                }
            } catch (RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Failed to process expired timesheet: timesheet_id=" + timesheet.getId());
            }
        }
        LOGGER.log(Level.INFO, "Total timesheets processed=" + archived.size() + ", timesheets_deleted=" + timesheetsDeleted + ", contracts_deleted=" + contractsDeleted);
    }
    
    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void signTimesheet(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        if (timesheet == null) {
            throw new ResourceNotFoundException("Timesheet", timesheetId);
        }
        validateEmployeeOwnsTimesheet(timesheet);

        if (timesheet.getStatus() != TimeSheetStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException("error.timesheetInvalidTransition", timesheet.getStatus());
        }

        timesheet.setStatus(TimeSheetStatus.SIGNED_BY_EMPLOYEE);
        timesheet.setSignedByEmployee(LocalDate.now());
        timesheetDao.update(timesheet);
    }
    
    @Override
    @RolesAllowed({"SUPERVISOR"})
    public void signAsSupervisor(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        if (timesheet == null) {
            throw new ResourceNotFoundException("Timesheet", timesheetId);
        }

        PersonEntity currentPerson = getCurrentPerson();
        if (!isSupervisorOnTimesheet(currentPerson, timesheet)) {
            throw new AccessDeniedException();
        }

        if (timesheet.getStatus() != TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
            throw new InvalidStateTransitionException("error.timesheetInvalidTransition", timesheet.getStatus());
        }

        timesheet.setStatus(TimeSheetStatus.SIGNED_BY_SUPERVISOR);
        timesheet.setSignedBySupervisor(LocalDate.now());
        timesheetDao.update(timesheet);
    }
    
    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public void requestChanges(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        if (timesheet == null) {
            throw new ResourceNotFoundException("Timesheet", timesheetId);
        }

        PersonEntity currentPerson = getCurrentPerson();
        if (!isSupervisorOrAssistantOnTimesheet(currentPerson, timesheet)) {
            throw new AccessDeniedException();
        }

        if (timesheet.getStatus() != TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
            throw new InvalidStateTransitionException("error.timesheetInvalidTransition", timesheet.getStatus());
        }

        timesheet.setStatus(TimeSheetStatus.IN_PROGRESS);
        timesheet.setSignedByEmployee(null);
        timesheetDao.update(timesheet);
    }
    
        
    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void revokeSignature(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        if (timesheet == null) {
            throw new ResourceNotFoundException("Timesheet", timesheetId);
        }
        validateEmployeeOwnsTimesheet(timesheet);

        if (timesheet.getStatus() != TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
            throw new InvalidStateTransitionException("error.timesheetInvalidTransition", timesheet.getStatus());
        }

        timesheet.setStatus(TimeSheetStatus.IN_PROGRESS);
        timesheet.setSignedByEmployee(null);
        timesheetDao.update(timesheet);
    }

    private double computeHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return 0.0;
        }
        return Duration.between(startTime, endTime).toMinutes() / 60.0;
    }

    private void validateNoOverlappingEntry(Long entryId, TimesheetEntity timesheet, TimesheetEntryDto candidate) {
        boolean overlapsExistingEntry = timesheet.getEntries().stream()
                .filter(existing -> entryId == null || !entryId.equals(existing.getId()))
                .filter(existing -> existing.getEntryDate().equals(candidate.getEntryDate()))
                .anyMatch(existing -> candidate.getStartTime().isBefore(existing.getEndTime())
                        && existing.getStartTime().isBefore(candidate.getEndTime()));

        if (overlapsExistingEntry) {
            throw new TimesheetEntryOverlapException();
        }
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
            throw new BusinessRuleViolationException("error.vacationHoursExceeded", vacationLimit);
        }
    }
    
    private void validateTimesheet(TimesheetEntity timesheet){
        
        if (timesheet == null) {
            throw new ResourceNotFoundException("Timesheet", null);
        }
        
        ContractEntity contract = timesheet.getContract();
        
        if (timesheet.getStatus() != TimeSheetStatus.IN_PROGRESS || contract == null || contract.getStatus() != sierra.tms.utils.enums.ContractStatus.STARTED) {
            throw new InvalidStateTransitionException("error.timesheetNotEditable");
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
    
    private boolean isSupervisorOnTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        return contract != null && contract.getSupervisor() != null
                && person != null && person.getId().equals(contract.getSupervisor().getId());
    }

    private boolean isSupervisorOrAssistantOnTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        if (contract == null || person == null) {
            return false;
        }
        return isSupervisorOnTimesheet(person, timesheet)
                || containsPerson(contract.getAssistants(), person.getId());
    }

    private boolean containsPerson(Set<PersonEntity> persons, Long personId) {
        return persons != null && persons.stream()
                .anyMatch(p -> p != null && personId.equals(p.getId()));
    }

    private void validateEmployeeOwnsTimesheet(TimesheetEntity timesheet) {
        PersonEntity currentPerson = getCurrentPerson();
        if (!isEmployeeOnTimesheet(currentPerson, timesheet)) {
            throw new AccessDeniedException("error.timesheet.notOwner", timesheet.getId());
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
