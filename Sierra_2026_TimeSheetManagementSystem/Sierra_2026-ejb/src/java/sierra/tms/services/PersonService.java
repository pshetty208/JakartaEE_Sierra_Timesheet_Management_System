 package sierra.tms.services;

import jakarta.ejb.Local;
import java.util.List;
import sierra.tms.dto.PersonDto;
import sierra.tms.utils.enums.RoleType;

@Local
public interface PersonService {
    
    void createPerson(PersonDto dto);

    PersonDto findById(Long id);

    List<PersonDto> findAll();

    PersonDto update(PersonDto dto);

    void delete(Long id);

    void giveConsent(Long personId);

    void changeCurrentPersonPreferredLanguage(String language);

    void assignRole(Long personId, RoleType role);

    void removeRole(Long personId, RoleType role);

    PersonDto getCurrentPerson();

}
