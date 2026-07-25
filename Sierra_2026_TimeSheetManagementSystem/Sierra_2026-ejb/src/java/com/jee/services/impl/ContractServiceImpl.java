package com.jee.services.impl;

import com.jee.dao.ContractDao;
import com.jee.dao.PersonDao;
import com.jee.dto.ContractDto;
import com.jee.entities.ContractEntity;
import com.jee.entities.PersonEntity;
import com.jee.entities.RoleEntity;
import com.jee.services.ContractService;
import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author pranavsudhir
 */
@Stateless
public class ContractServiceImpl implements ContractService {

    @EJB
    private ContractDao contractDao;

    @EJB
    private PersonDao personDao;

    @Resource
    private SessionContext sessionContext;

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
    public ContractDto getContractById(Long id) {
        ContractEntity entity = contractDao.findById(id);
        if (entity == null) {
            return null;
        }

        PersonEntity currentPerson = getCurrentPerson();
        if (currentPerson == null || !isAssignedToContract(currentPerson, entity)) {
            throw new EJBAccessException(
                    "The current user is not authorized to view this contract.");
        }

        return createDTO(entity);
    }

    private PersonEntity getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return personDao.findByEmailAddress(emailAddress);
    }

    private boolean isAssignedToContract(
            PersonEntity person,
            ContractEntity contract) {
        Long personId = person.getId();

        return belongsToPerson(contract.getEmployee(), personId)
                || belongsToPerson(contract.getSupervisor(), personId)
                || containsPerson(contract.getAssistants(), personId)
                || containsPerson(contract.getSecretaries(), personId);
    }

    private boolean containsPerson(Set<RoleEntity> roles, Long personId) {
        for (RoleEntity role : roles) {
            if (belongsToPerson(role, personId)) {
                return true;
            }
        }
        return false;
    }

    private boolean belongsToPerson(RoleEntity role, Long personId) {
        return personId.equals(role.getPerson().getId());
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
                createRoleIdSet(entity.getAssistants()),
                createRoleIdSet(entity.getSecretaries())
        );
    }

    private Set<Long> createRoleIdSet(Set<RoleEntity> roles) {
        Set<Long> roleIds = new LinkedHashSet<>();
        for (RoleEntity role : roles) {
            roleIds.add(role.getId());
        }
        return roleIds;
    }
}
