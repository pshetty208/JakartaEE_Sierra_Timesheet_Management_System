package sierra.tms.web;

import sierra.tms.dto.PersonDto;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import javax.management.relation.Role;
import sierra.tms.service.PersonService;

@Named
@ViewScoped
public class PersonBean implements Serializable {
    
    @EJB
    private PersonService service;
    
    private PersonDto person;
    
    private Role role;
    
    public void save() {
        service.save(person);
    }
    
    public PersonDto findById(Long id) {
        return service.findById(id);
    }

    public List<PersonDto> findAll() {
        return service.findAll();
    }
    
    public void update() {
        service.update(person);
    }
    
    public void delete(Long id) {
        service.delete(id);   
    }
    
    public void changeRole(PersonDto person) {
        service.changeRole(person.getId(), person.getRole().name());
    }
    
    public PersonDto getPerson() {
        return person;
    }

    public void setPerson(PersonDto person) {
        this.person = person;
    }

}
