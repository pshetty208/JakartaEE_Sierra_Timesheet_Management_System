package sierra.tms.service.impl;

import sierra.tms.dao.PersonDao;
import sierra.tms.dto.PersonDto;
import sierra.tms.entities.PersonEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;
import sierra.tms.service.PersonService;
import sierra.tms.utils.RoleType;


@Stateless
public class PersonServiceImpl implements PersonService{
    
    @EJB
    private PersonDao dao;
    
    @Override
    public void save(PersonDto personDto) {
        PersonEntity person = new PersonEntity();
        person.setFirstName(personDto.getFirstName());
        person.setLastName(personDto.getLastName());
        person.setEmailAddress(personDto.getEmailAddress());
        person.setDateOfBirth(personDto.getDateOfBirth());
        person.setConsent(personDto.getConsent());
        person.setRole(personDto.getRole());
        dao.save(person);
    }

    @Override
    public PersonDto findById(Long id) {
        PersonEntity person = dao.findById(id);
        if (person == null) {
            return null;
        }
        return createDTO(person);
    }

    @Override
    public List<PersonDto> findAll() {
        return dao.findAll().stream()
                .map(this::createDTO)
                .toList();

    }

    @Override
    public PersonDto update(PersonDto personDto) {
        PersonEntity person = dao.findById(personDto.getId());
        if (person == null) {
            return null;
        }
        person.setFirstName(personDto.getFirstName());
        person.setLastName(personDto.getLastName());
        person.setEmailAddress(personDto.getEmailAddress());
        person.setDateOfBirth(personDto.getDateOfBirth());
        person.setConsent(personDto.getConsent());
        person.setRole(personDto.getRole());
        PersonEntity updatedPerson = dao.update(person);
        return createDTO(updatedPerson);
    }

    @Override
    public void delete(Long id) {
        PersonEntity person = dao.findById(id);
        if (person == null) {
//            return null;
        }
        dao.delete(person);
    }

    @Override
    public void changeRole(Long personId, String roleType) {
        PersonEntity person = dao.findById(personId);  
        person.setRole(RoleType.valueOf(roleType.toUpperCase()));
        dao.update(person);
        
    }
    
    private PersonDto createDTO(PersonEntity entity) {
        return new PersonDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmailAddress(),
                entity.getDateOfBirth(),
                entity.getConsent(),
                entity.getRole()
        );
    }
}