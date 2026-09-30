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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.dao.PersonDao;
import sierra.tms.dao.TimesheetSignatureEventDao;
import sierra.tms.entities.PersonEntity;
import sierra.tms.entities.TimesheetSignatureEventEntity;
import sierra.tms.services.ContractHoursCalculationService;
import sierra.tms.services.ContractService;
import sierra.tms.utils.ConfigService;
import sierra.tms.utils.enums.ReportType;
import sierra.tms.utils.enums.TimeSheetStatus;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.TimesheetActorCapacity;
import sierra.tms.utils.enums.TimesheetAuthenticationMethod;
import sierra.tms.utils.enums.TimesheetSignatureAction;

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
    private TimesheetSignatureEventDao signatureEventDao;

    @EJB
    private ConfigService configService;

    @Resource
    private SessionContext sessionContext;

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
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
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public List<TimesheetDto> findAll() {
        PersonEntity currentPerson = getCurrentPerson();
        List<TimesheetEntity> timesheets = timesheetDao.findAll()
                .stream()
                .filter(t -> isAuthorizedToViewTimesheet(currentPerson, t))
                .toList();
        return createDTOsWithChangeRequestState(timesheets);
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public List<TimesheetDto> findByContractId(Long contractId) {
        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findByContractId(contractId)
                .stream()
                .filter(t -> isAuthorizedToViewTimesheet(currentPerson, t))
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
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
    @RolesAllowed({"EMPLOYEE"})
    public List<TimesheetDto> findForEmployeeSignatureRevocation() {
        if (!sessionContext.isCallerInRole("EMPLOYEE")) {
            return List.of();
        }

        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findAll()
                .stream()
                .filter(t -> isEmployeeOnTimesheet(currentPerson, t))
                .filter(t -> t.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE
                        && t.getContract().getStatus() == ContractStatus.STARTED)
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"SUPERVISOR"})
    public List<TimesheetDto> findForSupervisorSigning() {
        if (!sessionContext.isCallerInRole("SUPERVISOR")) {
            return List.of();
        }

        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findAll()
                .stream()
                .filter(t -> isSupervisorOnTimesheet(currentPerson, t))
                .filter(t -> t.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE)
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public List<TimesheetDto> findForChangeRequest() {
        if (!sessionContext.isCallerInRole("SUPERVISOR")
                && !sessionContext.isCallerInRole("ASSISTANT")) {
            return List.of();
        }

        PersonEntity currentPerson = getCurrentPerson();
        return timesheetDao.findAll()
                .stream()
                .filter(t -> isSupervisorOrAssistantOnTimesheet(currentPerson, t))
                .filter(t -> t.getStatus() == TimeSheetStatus.SIGNED_BY_EMPLOYEE)
                .map(this::createDTO)
                .toList();
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public Long addEntry(Long timesheetId, TimesheetEntryDto entry) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);
        validateEntryDateWithinRange(timesheet, entry);
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

        LOGGER.log(Level.INFO, "Timesheet entry created: entry_id={0}, timesheet_id={1}, actor_id={2}",
                new Object[]{entity.getId(), timesheetId, currentPersonId()});

        return entity.getId();
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void updateEntry(TimesheetEntryDto entry) {
        TimesheetEntryEntity entity = entryDao.findById(entry.getId());

        if (entity == null) {
            LOGGER.log(Level.WARNING, "Timesheet entry update rejected: entry_id={0}, reason=not_found",
                    entry.getId());
             throw new EntityNotFoundException("Timesheet Entry with id:" + entry.getId() + " not found");
        }

        TimesheetEntity timesheet = entity.getTimesheet();
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);
        validateEntryDateWithinRange(timesheet, entry);
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
        LOGGER.log(Level.INFO, "Timesheet entry updated: entry_id={0}, timesheet_id={1}, actor_id={2}",
                new Object[]{entity.getId(), timesheet.getId(), currentPersonId()});
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void deleteEntry(Long entryId) {
        TimesheetEntryEntity entity = entryDao.findById(entryId);
        if (entity == null) {
            LOGGER.log(Level.WARNING, "Timesheet entry deletion rejected: entry_id={0}, reason=not_found",
                    entryId);
            throw new EntityNotFoundException("Timesheet Entry with id:" + entryId + " not found");
        }

        TimesheetEntity timesheet = entity.getTimesheet();
        validateEmployeeOwnsTimesheet(timesheet);
        validateTimesheet(timesheet);

        timesheet.removeEntry(entity);
        entryDao.delete(entryId);
        LOGGER.log(Level.INFO, "Timesheet entry deleted: entry_id={0}, timesheet_id={1}, actor_id={2}",
                new Object[]{entryId, timesheet.getId(), currentPersonId()});
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
                || !currentPerson.isUniversityStaff()
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
            LOGGER.log(Level.WARNING, "Timesheet archive rejected: timesheet_id={0}, reason=not_found", id);
            throw new EntityNotFoundException("Timesheet not found: " + id);
        }

        PersonEntity currentPerson = getCurrentPerson();
        ContractEntity contract = entity.getContract();
        if (contract == null || currentPerson == null
                || !currentPerson.isUniversityStaff()
                || !containsPerson(contract.getSecretaries(), currentPerson.getId())) {
            LOGGER.log(Level.WARNING, "Timesheet archive rejected: timesheet_id={0}, reason=access_denied", id);
            throw new EJBAccessException(
                    "Only a secretary assigned to this contract may archive its timesheet.");
        }

        if (entity.getStatus() != TimeSheetStatus.SIGNED_BY_SUPERVISOR) {
            LOGGER.log(Level.WARNING, "Timesheet archive rejected: timesheet_id={0}, reason=invalid_status", id);
            throw new IllegalStateException("Only timesheets in SIGNED_BY_SUPERVISOR status can be archived.");
        }

        entity.setStatus(TimeSheetStatus.ARCHIVED);
        timesheetDao.update(entity);
        LOGGER.log(Level.INFO, "Timesheet archived: timesheet_id={0}, actor_id={1}",
                new Object[]{id, currentPerson.getId()});

        if (contract != null) {
            contractService.archiveContract(contract.getId());
        }
    }

    @Schedule(hour = "2", minute = "0", second = "0", persistent = true)
    public void deleteExpiredTimesheets() {
        LOGGER.log(Level.INFO, "Expired-timesheet cleanup started");
        int deletedTimesheets = 0;
        int deletedContracts = 0;
        try {
            List<TimesheetEntity> archived = timesheetDao.findByStatus(TimeSheetStatus.ARCHIVED);
            LocalDate today = LocalDate.now(configService.getTimeZone());

            for (TimesheetEntity timesheet : archived) {
                if (timesheet.getSignedBySupervisor() == null) {
                    continue;
                }

                ContractEntity contract = timesheet.getContract();
                int archiveDurationMonths = (contract != null && contract.getArchiveDuration() != null)
                        ? contract.getArchiveDuration() : configService.getDefaultArchiveDurationMonths();

                LocalDate expiryDate = timesheet.getSignedBySupervisor().plusMonths(archiveDurationMonths);
                if (today.isBefore(expiryDate)) {
                    continue;
                }

                Long timesheetId = timesheet.getId();
                timesheetDao.delete(timesheet);
                deletedTimesheets++;

                if (contract == null) {
                    continue;
                }
                boolean anyTimesheetsRemain = timesheetDao.findByContractId(contract.getId())
                        .stream()
                        .anyMatch(t -> !t.getId().equals(timesheetId));
                if (!anyTimesheetsRemain) {
                    contractDao.delete(contract);
                    deletedContracts++;
                }
            }
            LOGGER.log(Level.INFO,
                    "Expired-timesheet cleanup completed: timesheets_deleted={0}, contracts_deleted={1}",
                    new Object[]{deletedTimesheets, deletedContracts});
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE,
                    "Expired-timesheet cleanup failed: timesheets_deleted=" + deletedTimesheets
                    + ", contracts_deleted=" + deletedContracts,
                    exception);
            throw exception;
        }
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void signTimesheet(Long timesheetId) {
        TimesheetEntity timesheet = requireTimesheet(timesheetId);
        validateEmployeeOwnsTimesheet(timesheet);
        requireStatus(timesheet, TimeSheetStatus.IN_PROGRESS,
                "Only timesheets in IN_PROGRESS status can be signed by the employee.");
        requireContractStarted(timesheet,
                "Only timesheets for STARTED contracts can be signed by the employee.");

        PersonEntity currentPerson = getCurrentPerson();
        LocalDateTime signedAt = LocalDateTime.now();
        recordSignatureEvent(
                timesheet,
                currentPerson,
                TimesheetSignatureAction.EMPLOYEE_SIGNED,
                TimesheetActorCapacity.EMPLOYEE,
                signedAt);

        timesheet.setStatus(TimeSheetStatus.SIGNED_BY_EMPLOYEE);
        timesheet.setSignedByEmployee(signedAt.toLocalDate());
        timesheetDao.update(timesheet);
        LOGGER.log(Level.INFO, "Timesheet signed by employee: timesheet_id={0}, actor_id={1}",
                new Object[]{timesheetId, currentPerson.getId()});
    }

    @Override
    @RolesAllowed({"SUPERVISOR"})
    public void signAsSupervisor(Long timesheetId) {
        TimesheetEntity timesheet = requireTimesheet(timesheetId);

        PersonEntity currentPerson = getCurrentPerson();
        if (!isSupervisorOnTimesheet(currentPerson, timesheet)) {
            LOGGER.log(Level.WARNING,
                    "Supervisor signature rejected: timesheet_id={0}, reason=access_denied", timesheetId);
            throw new EJBAccessException("Only the assigned supervisor may sign this timesheet.");
        }
        requireStatus(timesheet, TimeSheetStatus.SIGNED_BY_EMPLOYEE,
                "Only timesheets in SIGNED_BY_EMPLOYEE status can be signed by the supervisor.");

        LocalDateTime signedAt = LocalDateTime.now();
        recordSignatureEvent(
                timesheet,
                currentPerson,
                TimesheetSignatureAction.SUPERVISOR_SIGNED,
                TimesheetActorCapacity.SUPERVISOR,
                signedAt);

        timesheet.setStatus(TimeSheetStatus.SIGNED_BY_SUPERVISOR);
        timesheet.setSignedBySupervisor(signedAt.toLocalDate());
        timesheetDao.update(timesheet);
        LOGGER.log(Level.INFO, "Timesheet signed by supervisor: timesheet_id={0}, actor_id={1}",
                new Object[]{timesheetId, currentPerson.getId()});
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public void requestChanges(Long timesheetId) {
        TimesheetEntity timesheet = requireTimesheet(timesheetId);

        PersonEntity currentPerson = getCurrentPerson();
        if (!isSupervisorOrAssistantOnTimesheet(currentPerson, timesheet)) {
            LOGGER.log(Level.WARNING,
                    "Timesheet change request rejected: timesheet_id={0}, reason=access_denied", timesheetId);
            throw new EJBAccessException(
                    "Only the assigned supervisor or assistant may request changes to this timesheet.");
        }
        requireStatus(timesheet, TimeSheetStatus.SIGNED_BY_EMPLOYEE,
                "Only timesheets in SIGNED_BY_EMPLOYEE status can be returned for changes.");

        LocalDateTime requestedAt = LocalDateTime.now();
        TimesheetActorCapacity actorCapacity = isSupervisorOnTimesheet(currentPerson, timesheet)
                ? TimesheetActorCapacity.SUPERVISOR
                : TimesheetActorCapacity.ASSISTANT;
        recordSignatureEvent(
                timesheet,
                currentPerson,
                TimesheetSignatureAction.CHANGES_REQUESTED,
                actorCapacity,
                requestedAt);

        timesheet.setStatus(TimeSheetStatus.IN_PROGRESS);
        timesheet.setSignedByEmployee(null);
        timesheetDao.update(timesheet);
        LOGGER.log(Level.INFO,
                "Timesheet changes requested: timesheet_id={0}, actor_id={1}, actor_capacity={2}",
                new Object[]{timesheetId, currentPerson.getId(), actorCapacity});
    }

    @Override
    @RolesAllowed({"EMPLOYEE"})
    public void revokeSignature(Long timesheetId) {
        TimesheetEntity timesheet = requireTimesheet(timesheetId);
        validateEmployeeOwnsTimesheet(timesheet);
        requireStatus(timesheet, TimeSheetStatus.SIGNED_BY_EMPLOYEE,
                "Only Employee signature can be revoked.");
        requireContractStarted(timesheet,
                "Only timesheets for STARTED contracts can have their employee signature revoked.");

        PersonEntity currentPerson = getCurrentPerson();
        LocalDateTime revokedAt = LocalDateTime.now();
        recordSignatureEvent(
                timesheet,
                currentPerson,
                TimesheetSignatureAction.EMPLOYEE_SIGNATURE_REVOKED,
                TimesheetActorCapacity.EMPLOYEE,
                revokedAt);

        timesheet.setStatus(TimeSheetStatus.IN_PROGRESS);
        timesheet.setSignedByEmployee(null);
        timesheetDao.update(timesheet);
        LOGGER.log(Level.INFO, "Employee signature revoked: timesheet_id={0}, actor_id={1}",
                new Object[]{timesheetId, currentPerson.getId()});
    }

    /** Shared find-or-throw used by every sign/revoke/request-changes transition. */
    private TimesheetEntity requireTimesheet(Long timesheetId) {
        TimesheetEntity timesheet = timesheetDao.findById(timesheetId);
        if (timesheet == null) {
            LOGGER.log(Level.WARNING, "Timesheet operation rejected: timesheet_id={0}, reason=not_found",
                    timesheetId);
            throw new EntityNotFoundException("Timesheet not found: " + timesheetId);
        }
        return timesheet;
    }

    private void requireStatus(TimesheetEntity timesheet, TimeSheetStatus required, String message) {
        if (timesheet.getStatus() != required) {
            LOGGER.log(Level.WARNING,
                    "Timesheet operation rejected: timesheet_id={0}, required_status={1}, actual_status={2}",
                    new Object[]{timesheet.getId(), required, timesheet.getStatus()});
            throw new IllegalStateException(message);
        }
    }

    private void requireContractStarted(TimesheetEntity timesheet, String message) {
        ContractEntity contract = timesheet.getContract();
        if (contract == null || contract.getStatus() != ContractStatus.STARTED) {
            LOGGER.log(Level.WARNING,
                    "Timesheet operation rejected: timesheet_id={0}, reason=contract_not_started",
                    timesheet.getId());
            throw new IllegalStateException(message);
        }
    }

    private void recordSignatureEvent(
            TimesheetEntity timesheet,
            PersonEntity actor,
            TimesheetSignatureAction action,
            TimesheetActorCapacity actorCapacity,
            LocalDateTime occurredAt) {
        TimesheetSignatureEventEntity signatureEvent = new TimesheetSignatureEventEntity(
                timesheet,
                actor,
                action,
                actorCapacity,
                occurredAt,
                TimesheetAuthenticationMethod.AUTHENTICATED_SESSION);
        timesheet.addSignatureEvent(signatureEvent);
        signatureEventDao.save(signatureEvent);
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
            throw new IllegalStateException(
                    "This entry overlaps time already reported for that date.");
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

    private void validateEntryDateWithinRange(TimesheetEntity timesheet, TimesheetEntryDto entry) {
        LocalDate entryDate = entry.getEntryDate();
        if (entryDate.isBefore(timesheet.getStartDate()) || entryDate.isAfter(timesheet.getEndDate())) {
            throw new IllegalStateException("This entry date is outside the timesheet's date range.");
        }
    }

    private PersonEntity getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return personDao.findByEmailAddress(emailAddress);
    }

    private Long currentPersonId() {
        PersonEntity person = getCurrentPerson();
        return person == null ? null : person.getId();
    }

    private boolean isAuthorizedToViewTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        if (sessionContext.isCallerInRole("ADMIN")) {
            return true;
        }

        ContractEntity contract = timesheet.getContract();
        if (contract == null || person == null) {
            return false;
        }
        Long personId = person.getId();
        return personId.equals(contract.getEmployee().getId())
                || person.isUniversityStaff()
                && (personId.equals(contract.getSupervisor().getId())
                || containsPerson(contract.getAssistants(), personId)
                || containsPerson(contract.getSecretaries(), personId));
    }

    private boolean isEmployeeOnTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        return contract != null && contract.getEmployee() != null
                && person != null && person.getId().equals(contract.getEmployee().getId());
    }

    private boolean isSupervisorOnTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        return contract != null && contract.getSupervisor() != null
                && person != null && person.isUniversityStaff()
                && person.getId().equals(contract.getSupervisor().getId());
    }

    private boolean isSupervisorOrAssistantOnTimesheet(PersonEntity person, TimesheetEntity timesheet) {
        ContractEntity contract = timesheet.getContract();
        if (contract == null || person == null || !person.isUniversityStaff()) {
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
            LOGGER.log(Level.WARNING,
                    "Timesheet entry operation rejected: timesheet_id={0}, reason=access_denied",
                    timesheet == null ? null : timesheet.getId());
            throw new EJBAccessException("Only the employee on this contract may manage its timesheet entries.");
        }
    }

    private TimesheetDto createDTO(TimesheetEntity entity) {
        return createDTO(entity, false);
    }

    private List<TimesheetDto> createDTOsWithChangeRequestState(List<TimesheetEntity> entities) {
        Set<Long> timesheetIds = entities.stream()
                .map(TimesheetEntity::getId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> changeRequestedIds = signatureEventDao.findTimesheetIdsWithLatestAction(
                timesheetIds,
                TimesheetSignatureAction.CHANGES_REQUESTED);
        return entities.stream()
                .map(entity -> createDTO(entity, changeRequestedIds.contains(entity.getId())))
                .toList();
    }

    private TimesheetDto createDTO(TimesheetEntity entity, boolean changesRequested) {
        TimesheetDto timesheet = new TimesheetDto(
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
        timesheet.setChangesRequested(
                changesRequested && entity.getStatus() == TimeSheetStatus.IN_PROGRESS);
        return timesheet;
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
