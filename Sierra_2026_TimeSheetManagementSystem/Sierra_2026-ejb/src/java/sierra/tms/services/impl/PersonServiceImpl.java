package sierra.tms.services.impl;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import java.util.List;
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

    @EJB
    private PersonDao personDao;

    @Resource
    private SessionContext sessionContext;

    @Override
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
    }

    @Override
    public PersonDto findById(Long id) {
        PersonEntity person = personDao.findById(id);
        return person == null ? null : convertToDto(person);
    }

    @Override
    public List<PersonDto> findAll() {
        return personDao.findAll()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
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

        PersonEntity updated = personDao.update(person);

        return convertToDto(updated);
    }

    @Override
    public void delete(Long id) {
        PersonEntity person = personDao.findById(id);

        if (person != null) {
            personDao.delete(person);
        }
    }

    @Override
    public void giveConsent(Long personId) {

        PersonEntity person = personDao.findById(personId);

        if (person != null) {
            person.setConsent(true);
            personDao.update(person);
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
        }
    }

    @Override
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
        }
    }

    @Override
    public void removeRole(Long personId, RoleType roleType) {

        PersonEntity person = personDao.findById(personId);

        if (person == null) {
            return;
        }

        person.getRoles().removeIf(role -> role.getRole() == roleType);

        personDao.update(person);
    }

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
}
