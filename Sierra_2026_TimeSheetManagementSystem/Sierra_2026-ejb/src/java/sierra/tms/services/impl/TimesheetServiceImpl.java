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
import sierra.tms.utils.enums.ContractStatus;

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
    public Long addEntry(Long timesheetId, TimesheetEntryDto entry) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);
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

        return entity.getId();
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void updateEntry(TimesheetEntryDto entry) {
        TimesheetEntryEntity entity = entryDao.findById(entry.getId());

        if (entity == null) {
             throw new EntityNotFoundException("Timesheet Entry with id:" + entry.getId() + " not found");
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
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void deleteEntry(Long entryId) {
        TimesheetEntryEntity entity = entryDao.findById(entryId);
        if (entity == null) {
            throw new EntityNotFoundException("Timesheet Entry with id:" + entryId + " not found");
        }

        TimesheetEntity timesheet = entity.getTimesheet();
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);

        timesheet.removeEntry(entity);
        entryDao.delete(entryId);
    }
    
    @Override
    @RolesAllowed({"SECRETARY"})
    public TimesheetDto getForPrinting(Long id) {
        TimesheetEntity entity = timesheetDao.findById(id);

        if (entity == null) {
            throw new EntityNotFoundException("Timesheet with id:" + id + " not found");
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
    
    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void signTimesheet(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        if (timesheet == null) {
            throw new EntityNotFoundException("Timesheet not found: " + timesheetId);
        }
        validateEmployeeOwnsTimesheet(timesheet);

        if (timesheet.getStatus() != TimeSheetStatus.IN_PROGRESS) {
            throw new IllegalStateException(" ");
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
            throw new EntityNotFoundException("Timesheet not found: " + timesheetId);
        }

        PersonEntity currentPerson = getCurrentPerson();
        if (!isSupervisorOnTimesheet(currentPerson, timesheet)) {
            throw new EJBAccessException(" ");
        }

        if (timesheet.getStatus() != TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
            throw new IllegalStateException(" ");
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
            throw new EntityNotFoundException("Timesheet not found: " + timesheetId);
        }

        PersonEntity currentPerson = getCurrentPerson();
        if (!isSupervisorOrAssistantOnTimesheet(currentPerson, timesheet)) {
            throw new EJBAccessException(" ");
        }

        if (timesheet.getStatus() != TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
            throw new IllegalStateException(" ");
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
            throw new EntityNotFoundException("Timesheet not found: " + timesheetId);
        }
        validateEmployeeOwnsTimesheet(timesheet);

        if (timesheet.getStatus() != TimeSheetStatus.SIGNED_BY_EMPLOYEE) {
            throw new IllegalStateException("Only Employee signature can be revoked.");
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
