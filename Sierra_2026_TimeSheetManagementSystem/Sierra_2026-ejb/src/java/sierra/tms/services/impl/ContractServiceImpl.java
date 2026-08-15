package sierra.tms.services.impl;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import sierra.tms.dao.ContractDao;
import sierra.tms.dao.PersonDao;
import sierra.tms.dto.ContractDto;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.PersonEntity;
import sierra.tms.services.VacationCalculationService;
import sierra.tms.services.ContractService;
import sierra.tms.utils.enums.ContractStatus;
import sierra.tms.utils.enums.RoleType;

@Stateless
public class ContractServiceImpl implements ContractService {

    @EJB
    private ContractDao contractDao;
    
    @EJB
    private PersonDao personDao;

    @Resource
    private SessionContext sessionContext;
    
    @EJB
    private VacationCalculationService calculationService;

    @Override
    public void createContract(ContractDto dto) {
        validateContract(dto);
        ContractEntity entity = toEntity(dto);
        contractDao.save(entity);
        dto.setId(entity.getId()); 
    }

    @Override
    public ContractDto findById(Long id) {
        ContractEntity entity = getRequiredContract(id);
        return toDto(entity);
    }

    @Override
    public List<ContractDto> findAll() {
        return contractDao.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ContractDto update(ContractDto dto) {
        if (dto == null || dto.getId() == null) {
            throw new IllegalArgumentException("Contract id is required to update a Contract.");
        }

        ContractEntity existing = getRequiredContract(dto.getId());
        validateContractForState(existing);
        toEntity(dto, existing);

        ContractEntity updated = contractDao.update(existing);
        return toDto(updated);
    }

    @Override
    public void delete(Long id) {
        ContractEntity contract = getRequiredContract(id);
        validateContractForState(contract);
        contractDao.delete(getRequiredContract(id));
    }

    private ContractEntity getRequiredContract(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Contract id must not be null.");
        }

        ContractEntity entity = contractDao.findById(id);
        if (entity == null) {
            throw new EntityNotFoundException("Contract not found: " + id);
        }
        return entity;
    }
    
    private void validateContract(ContractDto dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Contract must not be null.");
        }

        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Contract name is required.");
        }

        if (dto.getEmployeeId() == null) {
            throw new IllegalArgumentException(
                    "Employee is required.");
        }

        if (dto.getSupervisorId() == null) {
            throw new IllegalArgumentException(
                    "Supervisor is required.");
        }

        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException(
                    "Start date is required.");
        }

        if (dto.getEndDate() == null) {
            throw new IllegalArgumentException(
                    "End date is required.");
        }

        if (dto.getFrequency() == null) {
            throw new IllegalArgumentException(
                    "Frequency is required.");
        }
    }
    
    private void validateContractForState(ContractEntity contract) {
    if (contract.getStatus() != ContractStatus.PREPARED) {
        throw new IllegalStateException(
                "Only contracts in PREPARED status can be modified.");
        }
    }
    
    private void validateEmployee(PersonEntity employee) {
        boolean isEmployee = employee.getRoles()
                .stream()
                .anyMatch(role -> role.getRole() == RoleType.EMPLOYEE);

        if (!isEmployee) {
            throw new IllegalArgumentException(
                    "Selected person is not an employee.");
        }
    }
    
    private void validateSupervisor(PersonEntity supervisor) {  
        boolean isSupervisor = supervisor.getRoles()
                .stream()
                .anyMatch(role -> role.getRole() == RoleType.SUPERVISOR);

        if (!isSupervisor) {
            throw new IllegalArgumentException(
                    "Selected person is not a supervisor.");
        }
    }
    
    private void validatePersons(PersonEntity employee,
                             PersonEntity supervisor) {

        if (employee.getId().equals(supervisor.getId())) {
            throw new IllegalArgumentException(
                    "Employee and supervisor must be different persons.");
        }
    }

//    private ContractEntity toEntity(ContractDto dto) {
//        if (dto == null) {
//            throw new IllegalArgumentException("Contract data must not be null.");
//        }
//
//        ContractEntity entity = new ContractEntity();
//        toEntity(dto, entity);
//        return entity;
//    }
    
    private ContractEntity toEntity(ContractDto dto) {

        ContractEntity entity = new ContractEntity();
        entity.setStatus(ContractStatus.PREPARED);
        copyEditableFields(dto, entity);
        return entity;
    }
    
    private void copyEditableFields(ContractDto dto,
                                ContractEntity entity) { //change this to have a flag

        PersonEntity employee = personDao.findById(dto.getEmployeeId());
        PersonEntity supervisor = personDao.findById(dto.getSupervisorId());

        if (employee == null) {
            throw new IllegalArgumentException("Employee not found.");
        }

        if (supervisor == null) {
            throw new IllegalArgumentException("Supervisor not found.");
        }
        
        validateEmployee(employee);
        validateSupervisor(supervisor);

        entity.setEmployee(employee);
        entity.setSupervisor(supervisor);
        entity.setName(dto.getName());
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setFrequency(dto.getFrequency());
        entity.setHoursPerWeek(dto.getHoursPerWeek());
        entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        entity.setVacationDaysPerYear(dto.getVacationDaysPerYear());
        entity.setTerminationDate(dto.getTerminationDate());
        entity.setArchiveDuration(dto.getArchiveDuration());
    }

    private void toEntity(ContractDto dto, ContractEntity entity) {
        PersonEntity employee = personDao.findById(dto.getEmployeeId());
        PersonEntity supervisor = personDao.findById(dto.getSupervisorId());
        
        if (employee == null) {
            throw new IllegalArgumentException("Employee not found.");
        }

        if (supervisor == null) {
            throw new IllegalArgumentException("Supervisor not found.");
        }
        
        entity.setEmployee(employee);
        entity.setSupervisor(supervisor);
        entity.setName(dto.getName());
        entity.setStatus(ContractStatus.PREPARED);//CN5
        entity.setStartDate(dto.getStartDate());
        entity.setEndDate(dto.getEndDate());
        entity.setFrequency(dto.getFrequency());
        entity.setHoursPerWeek(dto.getHoursPerWeek());
        entity.setWorkingDaysPerWeek(dto.getWorkingDaysPerWeek());
        entity.setVacationDaysPerYear(dto.getVacationDaysPerYear());
        entity.setTerminationDate(dto.getTerminationDate());
        entity.setArchiveDuration(dto.getArchiveDuration());
    }

    private ContractDto toDto(ContractEntity entity) {

        ContractDto dto = new ContractDto();

        dto.setId(entity.getId());

        dto.setEmployeeId(entity.getEmployee().getId());
        dto.setSupervisorId(entity.getSupervisor().getId());

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
        dto.setVacationHours(calculationService.calculateVacationHours(entity.getId()));//CN4a

        return dto;
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public ContractDto getContractById(Long id) {
        ContractEntity contract = contractDao.findById(id);
        if (contract == null) {
            return null;
        }

        PersonEntity currentPerson = getCurrentPerson();
        if (currentPerson == null
                || !isAuthorizedToViewContract(currentPerson, contract)) {
            throw new EJBAccessException(
                    "The current user is not authorized to view this contract.");
        }

        return createDTO(contract);
    }

    private PersonEntity getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return personDao.findByEmailAddress(emailAddress);
    }

    private boolean isAuthorizedToViewContract(
            PersonEntity person,
            ContractEntity contract) {
        Long personId = person.getId();

        if (belongsToPerson(contract.getEmployee(), personId)) {
            return true;
        }

        boolean hasStaffContractRole
                = belongsToPerson(contract.getSupervisor(), personId)
                || containsPerson(contract.getAssistants(), personId)
                || containsPerson(contract.getSecretaries(), personId);

        return person.isUniversityStaff() && hasStaffContractRole;
    }

    private boolean containsPerson(Set<PersonEntity> persons, Long personId) {
        for (PersonEntity person : persons) {
            if (belongsToPerson(person, personId)) {
                return true;
            }
        }
        return false;
    }

    private boolean belongsToPerson(PersonEntity person, Long personId) {
        return person != null && personId.equals(person.getId());
    }

    private ContractDto createDTO(ContractEntity entity) {
        return new ContractDto(
                entity.getId(),
                entity.getEmployee().getId(),
                entity.getSupervisor().getId(),
                entity.getName(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getFrequency(),
                entity.getHoursPerWeek(),
                entity.getWorkingDaysPerWeek(),
                entity.getVacationDaysPerYear(),
                entity.getTerminationDate(),
                entity.getArchiveDuration(),
                createPersonIdSet(entity.getAssistants()),
                createPersonIdSet(entity.getSecretaries())
        );
    }

    private Set<Long> createPersonIdSet(Set<PersonEntity> people) {
        Set<Long> ids = new LinkedHashSet<>();
        for (PersonEntity person : people) {
            ids.add(person.getId());
        }
        return ids;
    }
}