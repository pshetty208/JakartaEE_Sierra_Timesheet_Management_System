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

    // Removed: with the single-role-per-user model, assignRole() already performs an atomic
    // swap (clears the existing role and sets the new one), so a separate removeRole() has no
    // valid use case - a person must always have exactly one role, and this never worked
    // correctly anyway (it threw when a role was assigned and silently did nothing otherwise).
    // void removeRole(Long personId, RoleType role);

    PersonDto getCurrentPerson();

}
