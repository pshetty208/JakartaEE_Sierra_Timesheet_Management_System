 package sierra.tms.service;

import jakarta.ejb.Remote;
import java.util.List;
import sierra.tms.dto.PersonDto;
import sierra.tms.utils.RoleType;


@Remote
public interface PersonService {
    
    public void save(PersonDto dto);

    public PersonDto findById(Long id);

    public List<PersonDto> findAll();
    
    public PersonDto update(PersonDto person);

    public void delete(Long id);
    
    public void changeRole(Long personId, String roleType);
        
}
