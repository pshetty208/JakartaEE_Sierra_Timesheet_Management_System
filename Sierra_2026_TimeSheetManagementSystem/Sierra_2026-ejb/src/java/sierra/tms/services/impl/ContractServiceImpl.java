package sierra.tms.services.impl;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.PersonDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.dto.ContractDto;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.PersonEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.exceptions.AccessDeniedException;
import sierra.tms.exceptions.BusinessRuleViolationException;
import sierra.tms.exceptions.InvalidStateTransitionException;
import sierra.tms.exceptions.ResourceNotFoundException;
import sierra.tms.exceptions.TerminationWarning;
import sierra.tms.exceptions.ValidationException;
import sierra.tms.services.ContractService;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.TimeSheetStatus;
import sierra.tms.services.ContractHoursCalculationService;
import sierra.tms.utils.enums.RoleType;

@Stateless
public class ContractServiceImpl implements ContractService {
    
    private static final Logger LOGGER = Logger.getLogger(ContractServiceImpl.class.getName());

    @EJB
    private ContractDao contractDao;
    
    @EJB
    private PersonDao personDao;
        
    @EJB
    private TimesheetDao timesheetDao;

    @Resource
    private SessionContext sessionContext;
    
    @EJB
    private ContractHoursCalculationService calculationService;

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public void createContract(ContractDto dto) {
        if (dto == null) {
            throw new ValidationException("MISSING_CONTRACT", "error.contract.required");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("MISSING_NAME", "error.contract.nameRequired");
        }

        validateRequiredFields(dto.getEmployeeId(), "Employee is required.");
        validateRequiredFields(dto.getSupervisorId(), "Supervisor is required.");
        validateRequiredFields(dto.getStartDate(), "Start date is required.");
        validateRequiredFields(dto.getEndDate(), "End date is required.");
        validateRequiredFields(dto.getFrequency(), "Frequency is required.");

        validateStartAndEndDates(dto.getStartDate(), dto.getEndDate());
        validateEmployeeHours(dto.getEmployeeId(), dto.getHoursPerWeek());
    
        ContractEntity contract = new ContractEntity();
        contract.setStatus(ContractStatus.PREPARED);
        contract.setName(dto.getName());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setFrequency(dto.getFrequency());
        contract.setHoursPerWeek(dto.getHoursPerWeek());
        contract.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        contract.setVacationDaysPerYear(dto.getVacationDaysPerYear());
        contract.setTerminationDate(dto.getTerminationDate());
        contract.setArchiveDuration(dto.getArchiveDuration());
        
        PersonEntity employee = personDao.findById(dto.getEmployeeId());
        if (employee == null) {
            throw new ResourceNotFoundException("Person", dto.getEmployeeId());
        }
        
//        boolean existingContract = contractDao.findByEmployee(dto.getEmployeeId())
//                .stream()
//                .anyMatch(c -> c.getStatus() == ContractStatus.PREPARED || c.getStatus() == ContractStatus.STARTED);
//        if (existingContract) {
//            throw new IllegalStateException("Employee already has a contract.");
//        }
        
        boolean isEmployee = employee.getRoles().stream().anyMatch(role -> role.getRole() == RoleType.EMPLOYEE);
        if (!isEmployee) {
            throw new ValidationException("NOT_EMPLOYEE_ROLE", "error.personNotEmployee", dto.getEmployeeId());
        }
        contract.setEmployee(employee);

        PersonEntity supervisor = personDao.findById(dto.getSupervisorId());
        if (supervisor == null) {
            throw new ResourceNotFoundException("Person", dto.getSupervisorId());
        }
        contract.setSupervisor(supervisor);
        contract.setAssistants(findPerson(dto.getAssistantRoleIds()));
        contract.setSecretaries(findPerson(dto.getSecretaryRoleIds()));
        
        contractDao.save(contract);
        LOGGER.log(Level.INFO, "Contract created: contract_id=" + contract.getId() + ", employee=" + employee.getId() + ", supervisor=" + supervisor.getId());
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public ContractDto findById(Long id) {
        ContractEntity entity = getRequiredContract(id);
        if (!sessionContext.isCallerInRole("ADMIN")) {
            PersonEntity currentPerson = getCurrentPerson();
            if (currentPerson == null || !isAuthorizedToViewContract(currentPerson, entity)) {
                throw new AccessDeniedException("error.contract.notAuthorizedToView", id);
            }
        }
        return createDto(entity);
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public List<ContractDto> findAll() {
        boolean isAdmin = sessionContext.isCallerInRole("ADMIN");
        PersonEntity person = isAdmin ? null : getCurrentPerson();
        return contractDao.findAll().stream()
            .filter(c -> isAdmin || isAuthorizedToViewContract(person, c))
            .map(this::createDto)
            .toList();
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public ContractDto update(ContractDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Contract cannot be empty.");
        }

        ContractEntity contract = getRequiredContract(dto.getId());
        PersonEntity currentPerson = getCurrentPerson();
        if (!isAuthorizedToManageContract(currentPerson, contract)) {
            throw new EJBAccessException("The current user is not authorized to update this contract.");
        }

        validateState(contract);
        validateStartAndEndDates(dto.getStartDate(), dto.getEndDate()); 
        contract.setName(dto.getName());
        contract.setStartDate(dto.getStartDate());
        contract.setEndDate(dto.getEndDate());
        contract.setFrequency(dto.getFrequency());
        contract.setHoursPerWeek(dto.getHoursPerWeek());
        contract.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        contract.setVacationDaysPerYear(dto.getVacationDaysPerYear());
        contract.setTerminationDate(dto.getTerminationDate());
        contract.setArchiveDuration(dto.getArchiveDuration());
        
        if (dto.getEmployeeId() != null) {
            PersonEntity employee = personDao.findById(dto.getEmployeeId());
            if (employee == null) {
                throw new EntityNotFoundException("Employee not found: "+ dto.getEmployeeId());
            }
            contract.setEmployee(employee);
        }
        
        if (dto.getSupervisorId() != null) {
            PersonEntity supervisor = personDao.findById(dto.getSupervisorId());
            if (supervisor == null) {
                throw new EntityNotFoundException("Supervisor not found: "+ dto.getSupervisorId());
            }
            contract.setSupervisor(supervisor);
        }
        
        if (dto.getAssistantRoleIds() != null) {
            contract.setAssistants(findPerson(dto.getAssistantRoleIds()));
        }
        
        if (dto.getSecretaryRoleIds() != null) {
            contract.setSecretaries(findPerson(dto.getSecretaryRoleIds()));
        }

        ContractEntity updated = contractDao.update(contract);
        LOGGER.log(Level.INFO, "Contract updated: contract_id=" + contract.getId());

        return createDto(updated);
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public void delete(Long id) {
        ContractEntity contract = getRequiredContract(id);
        if (!isAuthorizedToManageContract(getCurrentPerson(), contract)) {
            throw new EJBAccessException("The current user is not authorized to delete this contract.");
        }
        validateState(contract);
        contractDao.delete(getRequiredContract(id));
        LOGGER.log(Level.INFO, "Contract deleted: contract_id=" + contract.getId());

    }
    
    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public void startContract(Long id) {
        ContractEntity contract = getRequiredContract(id);
        if (!isAuthorizedToManageContract(getCurrentPerson(), contract)) {
            throw new EJBAccessException("The current user is not authorized to start this contract.");
        }
        if (contract.getStatus() != ContractStatus.PREPARED) {
            throw new InvalidStateTransitionException("error.contractNotPrepared");
        }
        contract.setStatus(ContractStatus.STARTED);
        List<TimesheetEntity> generateTimesheets = generateTimesheets(contract);
        for (TimesheetEntity timesheet : generateTimesheets) {
            timesheetDao.save(timesheet);
        }
        LOGGER.log(Level.INFO, "Contract started: contract_id=" + contract.getId() + " and " + generateTimesheets.size() + " timesheets created");
    }
    
    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public void archiveContract(Long contractId) {
        ContractEntity contract = getRequiredContract(contractId);
        List<TimesheetEntity> timesheets = timesheetDao.findByContractId(contractId);
        boolean allArchived = !timesheets.isEmpty() && timesheets.stream().allMatch(t -> t.getStatus() == TimeSheetStatus.ARCHIVED);
        if (allArchived) {
            contract.setStatus(ContractStatus.ARCHIVED);
        LOGGER.log(Level.INFO, "Contract archived: contract_id=" + contract.getId());
        }
    }
    
    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT"})
    public void terminateContract(Long id, boolean confirmed) {
        ContractEntity contract = getRequiredContract(id);
        if (!isAuthorizedToManageContract(getCurrentPerson(), contract)) {
            throw new EJBAccessException("The current user is not authorized to terminate this contract.");
        }
        if (contract.getStatus() != ContractStatus.STARTED) {
            throw new InvalidStateTransitionException("error.contractNotStarted");
        }

        List<TimesheetEntity> timesheets = timesheetDao.findByContractId(id);

        boolean hasBlockingTimesheet = timesheets.stream().anyMatch(t -> t.getStatus() != TimeSheetStatus.IN_PROGRESS
                        && t.getStatus() != TimeSheetStatus.SIGNED_BY_SUPERVISOR
                        && t.getStatus() != TimeSheetStatus.ARCHIVED);
        if (hasBlockingTimesheet) {
            throw new BusinessRuleViolationException("error.contract.blockedByPendingSupervisorSignature", id);
        }
        
        if (!confirmed) {
        boolean hasInProgressWithEntries = timesheets.stream()
                .anyMatch(t -> t.getStatus() == TimeSheetStatus.IN_PROGRESS && !t.getEntries().isEmpty());
            if (hasInProgressWithEntries) {
                throw new TerminationWarning("warning.contract.terminationWillDeleteInProgressEntries", id);
            }
        }

        timesheets.stream().filter(t -> t.getStatus() == TimeSheetStatus.IN_PROGRESS).forEach(timesheetDao::delete);// TS4

        contract.setStatus(ContractStatus.TERMINATED);
        LocalDate date = LocalDate.now();
        contract.setTerminationDate(date);
        LOGGER.log(Level.INFO, "Contract terminated: contract_id=" + contract.getId() + ", at " + date);
    }
    
    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public ContractDto getContractDetails(Long contractId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    @RolesAllowed({"SECRETARY", "ADMIN"})
    public ContractDto getContractForPrinting(Long id) {
        return createDto(getRequiredContract(id));
    }
    
    private ContractEntity getRequiredContract(Long id) {
        if (id == null) {
            throw new ValidationException("MISSING_ID", "error.requiredField", "Contract id");
        }

        ContractEntity entity = contractDao.findById(id);
        if (entity == null) {
            throw new ResourceNotFoundException("Contract", id);
        }
        return entity;
    }
    
    private void validateRequiredFields(Object value, String message) {
        if (value == null) {
            throw new ValidationException("MISSING_FIELD", "error.requiredField", message);    
        }
    }
    
    private void validateStartAndEndDates(LocalDate startDate, LocalDate endDate) {
        if (!startDate.equals(startDate.withDayOfMonth(1))) {
            throw new ValidationException("INVALID_PERIOD", "error.invalidContractPeriod");
        }
        if (!endDate.equals(endDate.with(TemporalAdjusters.lastDayOfMonth()))) {
            throw new ValidationException("INVALID_END_DATE", "error.contract.endDateNotLastOfMonth", endDate);
        }
    }
    
    private void validateEmployeeHours(Long employeeId, Double hours) {
        if (hours == null || hours < 0) {
            throw new ValidationException("INVALID_HOURS", "error.requiredField", "Hours per week");
        }

        double existingHours = contractDao.findByEmployee(employeeId)
                .stream()
                .filter(contract -> contract.getStatus() == ContractStatus.PREPARED || contract.getStatus() == ContractStatus.STARTED)
                .mapToDouble(ContractEntity::getHoursPerWeek)
                .sum();

        if (existingHours + hours > 20) {
            LOGGER.log(Level.WARNING, "Rejected contract for employee: employee_id=" + employeeId + " as working hours has exceeded 20 hours per week.");            
            throw new BusinessRuleViolationException("error.contractHoursCapExceeded", existingHours + hours, 20);
        }

    }
    
    private boolean isAuthorizedToViewContract(PersonEntity person, ContractEntity contract) {
        if (person == null || person.getId() == null) {
            throw new AccessDeniedException();
        }
        
        Long personId = person.getId();

        return personId.equals(contract.getEmployee().getId())
                || personId.equals(contract.getSupervisor().getId())
                || containsPerson(contract.getAssistants(), personId)
                || containsPerson(contract.getSecretaries(), personId);
        
    }

    private boolean isAuthorizedToManageContract(PersonEntity person, ContractEntity contract) {
        if (person == null || person.getId() == null) {
            return false;
        }

        Long personId = person.getId();
        return personId.equals(contract.getSupervisor().getId())
                || containsPerson(contract.getAssistants(), personId);
    }

    private boolean containsPerson(Set<PersonEntity> persons, Long personId) {
        return persons != null && persons.stream()
                        .anyMatch(person -> person != null && personId.equals(person.getId()));
    }
    
    private PersonEntity getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return personDao.findByEmailAddress(emailAddress);
    }
    
    private void validateState(ContractEntity contract) {
        if (contract.getStatus() != ContractStatus.PREPARED) {
            throw new InvalidStateTransitionException("error.contractNotPrepared");
        }
    }
    
    private Set<PersonEntity> findPerson(Set<Long> personIds) {
        Set<PersonEntity> persons = new LinkedHashSet<>();

        if (personIds == null) {
            return persons;
        }

        for (Long personId : personIds) {
            if (personId == null) {
                continue;
            }

            PersonEntity person = personDao.findById(personId);
            if (person == null) {
                throw new ResourceNotFoundException("Person", personId);
            }
            persons.add(person);
        }
        return persons;
    }
    
    private List<TimesheetEntity> generateTimesheets(ContractEntity contract) {
        List<TimesheetEntity> timesheets = new ArrayList<>();
        LocalDate periodStart = contract.getStartDate();
        LocalDate contractEnd = contract.getEndDate();

        while (!periodStart.isAfter(contractEnd)) {
            LocalDate periodEnd = switch (contract.getFrequency()) {
                case WEEKLY -> periodStart.plusDays(6);
                case MONTHLY -> periodStart.with(TemporalAdjusters.lastDayOfMonth());
            };
            if (periodEnd.isAfter(contractEnd)) {
                periodEnd = contractEnd;
            }

            TimesheetEntity timesheet = new TimesheetEntity();
            timesheet.setStatus(TimeSheetStatus.IN_PROGRESS);
            timesheet.setStartDate(periodStart);
            timesheet.setEndDate(periodEnd);
            timesheet.setContract(contract);
            timesheets.add(timesheet);

            periodStart = periodEnd.plusDays(1);
        }
        return timesheets;
    }
    
    public void archiveContractIfComplete(Long contractId) {
        ContractEntity contract = getRequiredContract(contractId);
        List<TimesheetEntity> timesheets = timesheetDao.findByContractId(contractId);
        boolean allArchived = !timesheets.isEmpty()
                && timesheets.stream()
                        .allMatch(t -> t.getStatus() == TimeSheetStatus.ARCHIVED);
        if (allArchived) {
            contract.setStatus(ContractStatus.ARCHIVED);
        }
    }
    
    private ContractDto createDto(ContractEntity entity) {
        ContractDto dto = new ContractDto();
        dto.setId(entity.getId());

        if (entity.getEmployee() != null) {
            dto.setEmployeeId(entity.getEmployee().getId());
        }

        if (entity.getSupervisor() != null) {
            dto.setSupervisorId(entity.getSupervisor().getId());
        }

        dto.setName(entity.getName());
        dto.setStatus(entity.getStatus());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setFrequency(entity.getFrequency());
        dto.setHoursPerWeek(entity.getHoursPerWeek());
        dto.setWorkingDaysPerWeek(entity.getWorkingDaysPerWeek());
        dto.setVacationDaysPerYear(entity.getVacationDaysPerYear());
        dto.setTerminationDate(entity.getTerminationDate());
        dto.setArchiveDuration(entity.getArchiveDuration());
        dto.setVacationHours(calculationService.calculateVacationHours(entity));

        dto.setAssistantRoleIds(entity.getAssistants()
                        .stream()
                        .map(PersonEntity::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));

        dto.setSecretaryRoleIds(entity.getSecretaries()
                        .stream()
                        .map(PersonEntity::getId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)));
        
        double reportedHours = calculationService.calculateTotalReportedHoursForContract(entity.getId());
        double hoursDue = calculationService.calculateTotalHoursDueForContract(entity.getId());
            
        dto.setTotalHoursDue(hoursDue);
        dto.setRemainingHours(calculationService.calculateRemainingHours(hoursDue,reportedHours));

        return dto;
    }

}
