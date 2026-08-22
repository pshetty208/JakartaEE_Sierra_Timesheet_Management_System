package sierra.tms.services.impl;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBAccessException;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import sierra.tms.dao.PersonDao;
import sierra.tms.dto.PersonDto;
import sierra.tms.entities.PersonEntity;
import sierra.tms.entities.RoleEntity;
import sierra.tms.services.PersonService;
import sierra.tms.utils.enums.RoleType;

@Stateless
public class PersonServiceImpl implements PersonService {
    
    private static final Logger LOGGER = Logger.getLogger(PersonServiceImpl.class.getName());

    @EJB
    private PersonDao personDao;

    @Resource
    private SessionContext sessionContext;

    @Override
    @RolesAllowed({"ADMIN"})
    public void createPerson(PersonDto dto) {
        PersonEntity person = new PersonEntity();

        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setEmailAddress(dto.getEmailAddress());
        person.setDateOfBirth(dto.getDateOfBirth());

        if (dto.getConsent() != null) {
            person.setConsent(dto.getConsent());
        }

        if (dto.getPreferredLanguage() != null) {
            person.setPreferredLanguage(dto.getPreferredLanguage());
        }

        person.setUniversityStaff(dto.isUniversityStaff());

        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one role for the user.");
        }

        for (RoleType roleType : dto.getRoles()) {
            RoleEntity role = new RoleEntity();
            role.setRole(roleType);
            person.addRole(role);
        }

        personDao.save(person);
        LOGGER.log(Level.INFO, "New user created: person_id={0}", person.getId());
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public PersonDto findById(Long id) {
        PersonEntity person = personDao.findById(id);
        return person == null ? null : convertToDto(person);
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public List<PersonDto> findAll() {
        return personDao.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public PersonDto update(PersonDto dto) {
        checkAccessLevel(dto.getId());
        PersonEntity person = personDao.findById(dto.getId());

        if (person == null) {
            return null;
        }

        person.setFirstName(dto.getFirstName());
        person.setLastName(dto.getLastName());
        person.setEmailAddress(dto.getEmailAddress());
        person.setDateOfBirth(dto.getDateOfBirth());

        if (dto.getConsent() != null) {
            person.setConsent(dto.getConsent());
        }

        if (dto.getPreferredLanguage() != null) {
            person.setPreferredLanguage(dto.getPreferredLanguage());
        }

        person.setUniversityStaff(dto.isUniversityStaff());

        PersonEntity updated = personDao.update(person);
        LOGGER.log(Level.INFO, "Upated user details: user_id={0}", dto.getId());
        return convertToDto(updated);
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public void delete(Long id) {
        PersonEntity person = personDao.findById(id);

        if (person != null) {
            personDao.delete(person);
            LOGGER.log(Level.WARNING, "Deleted user: user_id={0}", id);
        }
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public void giveConsent(Long personId) {
        checkAccessLevel(personId);
        PersonEntity person = personDao.findById(personId);

        if (person != null) {
            person.setConsent(true);
            personDao.update(person);
        }
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public void changePreferredLanguage(Long personId, String language) {
        checkAccessLevel(personId);
        PersonEntity person = personDao.findById(personId);

        if (person != null) {
            person.setPreferredLanguage(language);
            personDao.update(person);
        }
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public void assignRole(Long personId, RoleType roleType) {

        PersonEntity person = personDao.findById(personId);

        if (person == null) {
            return;
        }

        boolean exists = person.getRoles()
                .stream()
                .anyMatch(role -> role.getRole() == roleType);

        if (!exists) {
            RoleEntity role = new RoleEntity();
            role.setRole(roleType);
            person.addRole(role);
            personDao.update(person);
            LOGGER.log(Level.INFO, "Roles assigned to user: user_id={0}", personId);
        }
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public void removeRole(Long personId, RoleType roleType) {

        PersonEntity person = personDao.findById(personId);

        if (person == null) {
            return;
        }

        person.getRoles().removeIf(role -> role.getRole() == roleType);

        personDao.update(person);
        LOGGER.log(Level.WARNING, "Roles removed from user: user_id={0}", personId);
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN" })
    public PersonDto getCurrentPerson() {

        String emailAddress = sessionContext
                .getCallerPrincipal()
                .getName();

        return findByEmailAddress(emailAddress);
    }
    
    private void checkAccessLevel(Long personId) {
        if (sessionContext.isCallerInRole("ADMIN")) {
            return;
        }
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        PersonEntity currentPerson = personDao.findByEmailAddress(emailAddress);
        if (currentPerson == null || !currentPerson.getId().equals(personId)) {
            throw new EJBAccessException("You may only manage your own person record.");
        }
    }

    private PersonDto findByEmailAddress(String emailAddress) {
        PersonEntity entity = personDao.findByEmailAddress(emailAddress);
        return entity == null ? null : createDTO(entity);
    }

    private PersonDto convertToDto(PersonEntity person) {
        return createDTO(person);
    }

    private PersonDto createDTO(PersonEntity entity) {
        PersonDto dto = new PersonDto();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setEmailAddress(entity.getEmailAddress());
        dto.setDateOfBirth(entity.getDateOfBirth());
        dto.setConsent(entity.isConsent());
        dto.setPreferredLanguage(entity.getPreferredLanguage());
        dto.setUniversityStaff(entity.isUniversityStaff());
        dto.setRoles(entity.getRoles()
                        .stream()
                        .map(RoleEntity::getRole)
                        .collect(Collectors.toList()));

        return dto;
    }
}