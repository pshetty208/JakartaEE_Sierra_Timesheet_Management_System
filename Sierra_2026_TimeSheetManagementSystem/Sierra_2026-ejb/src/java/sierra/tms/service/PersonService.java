 package sierra.tms.service;

import jakarta.ejb.Remote;
import java.util.List;
import sierra.tms.dto.PersonDto;
import sierra.tms.utils.RoleType;

/**
 *
 * @author prajnashetty
 */
@Remote
public interface PersonService {
    
    void createPerson(PersonDto dto);

    PersonDto findById(Long id);

    List<PersonDto> findAll();

    PersonDto update(PersonDto dto);

    void delete(Long id);

    void giveConsent(Long personId);

    void changePreferredLanguage(Long personId, String language);

    void assignRole(Long personId, RoleType role);

    void removeRole(Long personId, RoleType role);

}
