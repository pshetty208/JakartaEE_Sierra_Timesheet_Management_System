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
import sierra.tms.i18n.LanguageResolver;
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
            person.setPreferredLanguage(
                    LanguageResolver.normalize(dto.getPreferredLanguage()));
        }

        person.setUniversityStaff(dto.isUniversityStaff());

        if (dto.getRoles() == null || dto.getRoles().size() != 1) {
            throw new IllegalArgumentException("Exactly one role is required.");
        }

        validateStaffRoles(dto.isUniversityStaff(), dto.getRoles());

        RoleEntity role = new RoleEntity();
        role.setRole(dto.getRoles().get(0));
        person.addRole(role);

        personDao.save(person);
        LOGGER.log(Level.INFO, "Person created: person_id={0}", person.getId());
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public PersonDto findById(Long id) {
        requireUniversityStaffCaller();
        PersonEntity person = personDao.findById(id);
        return person == null ? null : convertToDto(person);
    }

    @Override
    @RolesAllowed({"SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public List<PersonDto> findAll() {
        requireUniversityStaffCaller();
        return personDao.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public PersonDto update(PersonDto dto) {

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
            person.setPreferredLanguage(
                    LanguageResolver.normalize(dto.getPreferredLanguage()));
        }

        person.setUniversityStaff(dto.isUniversityStaff());
        validateStaffRoles(person.isUniversityStaff(), person.getRoles().stream()
                .map(RoleEntity::getRole)
                .collect(Collectors.toList()));

        PersonEntity updated = personDao.update(person);
        LOGGER.log(Level.INFO, "Person updated: person_id={0}", person.getId());

        return convertToDto(updated);
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public void delete(Long id) {
        PersonEntity person = personDao.findById(id);

        if (person != null) {
            personDao.delete(person);
            LOGGER.log(Level.INFO, "Person deleted: person_id={0}", id);
        } else {
            LOGGER.log(Level.WARNING, "Person deletion skipped because the record was not found: person_id={0}", id);
        }
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public void giveConsent(Long personId) {
        PersonEntity person = personDao.findById(personId);

        if (person != null) {
            String callerEmail = sessionContext.getCallerPrincipal().getName();
            if (!callerEmail.equalsIgnoreCase(person.getEmailAddress())) {
                throw new EJBAccessException("Users may only give consent for their own account.");
            }
            person.setConsent(true);
            personDao.update(person);
            LOGGER.log(Level.INFO, "Person consent recorded: person_id={0}", personId);
        }
    }

    @Override
    @RolesAllowed({
        "EMPLOYEE",
        "SUPERVISOR",
        "ASSISTANT",
        "SECRETARY",
        "ADMIN"
    })
    public void changeCurrentPersonPreferredLanguage(String language) {
        String normalizedLanguage = LanguageResolver.normalize(language);
        String emailAddress = sessionContext
                .getCallerPrincipal()
                .getName();

        PersonEntity person = personDao.findByEmailAddress(emailAddress);

        if (person != null) {
            person.setPreferredLanguage(normalizedLanguage);
            personDao.update(person);
            LOGGER.log(Level.INFO, "Preferred language changed: person_id={0}", person.getId());
        }
    }

    @Override
    @RolesAllowed({"ADMIN"})
    public void assignRole(Long personId, RoleType roleType) {

        PersonEntity person = personDao.findById(personId);

        if (person == null) {
            return;
        }

        validateStaffRoles(person.isUniversityStaff(), List.of(roleType));

        boolean alreadyAssigned = person.getRoles()
                .stream()
                .anyMatch(role -> role.getRole() == roleType);

        if (!alreadyAssigned) {
            person.getRoles().forEach(existingRole -> existingRole.setPerson(null));
            person.getRoles().clear();
            RoleEntity role = new RoleEntity();
            role.setRole(roleType);
            person.addRole(role);
            personDao.update(person);
            LOGGER.log(Level.INFO, "Person role assigned: person_id={0}, role={1}",
                    new Object[]{personId, roleType});
        }
    }

    // Removed: with the single-role-per-user model, assignRole() already performs an atomic
    // swap (clears the existing role and sets the new one), so a separate removeRole() has no
    // valid use case - a person must always have exactly one role, and this never worked
    // correctly anyway (it threw when a role was assigned and silently did nothing otherwise).
    // @Override
    // @RolesAllowed({"ADMIN"})
    // public void removeRole(Long personId, RoleType roleType) {
    //
    //     PersonEntity person = personDao.findById(personId);
    //
    //     if (person == null) {
    //         return;
    //     }
    //
    //     boolean assigned = person.getRoles()
    //             .stream()
    //             .anyMatch(role -> role.getRole() == roleType);
    //     if (assigned) {
    //         throw new IllegalStateException(
    //                 "A person's only role cannot be removed; assign a replacement role instead.");
    //     }
    // }

    @Override
    @RolesAllowed({
        "EMPLOYEE",
        "SUPERVISOR",
        "ASSISTANT",
        "SECRETARY",
        "ADMIN"
    })
    public PersonDto getCurrentPerson() {

        String emailAddress = sessionContext
                .getCallerPrincipal()
                .getName();

        return findByEmailAddress(emailAddress);
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

    private void requireUniversityStaffCaller() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        PersonEntity currentPerson = personDao.findByEmailAddress(emailAddress);
        if (currentPerson == null || !currentPerson.isUniversityStaff()) {
            throw new EJBAccessException("This operation is restricted to university staff.");
        }
    }

    private void validateStaffRoles(boolean universityStaff, List<RoleType> roles) {
        boolean containsStaffRole = roles.stream().anyMatch(role ->
                role == RoleType.SUPERVISOR
                || role == RoleType.ASSISTANT
                || role == RoleType.SECRETARY
                || role == RoleType.ADMIN);
        if (containsStaffRole && !universityStaff) {
            throw new IllegalArgumentException(
                    "Supervisor, assistant, secretary, and admin roles require university staff status.");
        }
    }
}
