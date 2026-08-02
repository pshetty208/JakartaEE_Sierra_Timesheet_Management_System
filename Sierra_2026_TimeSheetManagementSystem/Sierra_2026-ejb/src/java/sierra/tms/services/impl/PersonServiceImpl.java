package sierra.tms.service.impl;

import sierra.tms.dao.PersonDao;
import sierra.tms.dto.PersonDto;
import sierra.tms.entities.PersonEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;
import java.util.stream.Collectors;
import sierra.tms.entities.RoleEntity;
import sierra.tms.service.PersonService;
import sierra.tms.utils.RoleType;

@Stateless
public class PersonServiceImpl implements PersonService{

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
        if(dto.getConsent() != null) {
            person.setConsent(dto.getConsent());
        }
        if(dto.getPreferredLanguage() != null) {
            person.setPreferredLanguage(dto.getPreferredLanguage());
        }
        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Select at least one role for the user.");
        }
        for(RoleType roleType : dto.getRoles()) {
                RoleEntity role = new RoleEntity();
                role.setRole(roleType);
                person.addRole(role);
        }
        personDao.save(person);
    }


    @Override
    public PersonDto findById(Long id) {
        PersonEntity person = personDao.findById(id);
        if (person == null) {
            return null;
        }
        return convertToDto(person);
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
    public void changePreferredLanguage(Long personId, String language) {
        PersonEntity person = personDao.findById(personId);
        if (person != null) {
            person.setPreferredLanguage(language);
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
                .anyMatch(r -> r.getRole() == roleType);

        if (!exists) {
            RoleEntity role = new RoleEntity();
            role.setRole(roleType);
            role.setPerson(person);
            person.getRoles().add(role);
            personDao.update(person);
        }
    }


    @Override
    public void removeRole(Long personId, RoleType roleType) {
        PersonEntity person = personDao.findById(personId);
        if (person == null) {
            return;
        }
        person.getRoles()
                .removeIf(role -> role.getRole() == roleType);
        personDao.update(person);
    }


    private PersonDto convertToDto(PersonEntity person) {

        PersonDto dto = new PersonDto();
        dto.setId(person.getId());
        dto.setFirstName(person.getFirstName());
        dto.setLastName(person.getLastName());
        dto.setEmailAddress(person.getEmailAddress());
        dto.setDateOfBirth(person.getDateOfBirth());
        dto.setConsent(person.isConsent());
        List<RoleType> roles = person.getRoles()
                .stream()
                .map(RoleEntity::getRole)
                .collect(Collectors.toList());

        dto.setRoles(roles);
        return dto;
    }

    @Override
    @RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY", "ADMIN"})
    public PersonDto getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return findByEmailAddress(emailAddress);
    }

    private PersonDto findByEmailAddress(String emailAddress) {
        PersonEntity entity = dao.findByEmailAddress(emailAddress);
        return entity == null ? null : createDTO(entity);
    }

    private PersonDto createDTO(PersonEntity entity) {
        return new PersonDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDateOfBirth(),
                entity.getEmailAddress(),
                entity.isUniversityStaff(),
                entity.isConsent()
        );
    }
}
