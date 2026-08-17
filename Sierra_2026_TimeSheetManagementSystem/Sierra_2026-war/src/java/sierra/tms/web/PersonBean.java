package sierra.tms.web;

import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import sierra.tms.dto.PersonDto;
import sierra.tms.services.PersonService;
import sierra.tms.utils.enums.RoleType;

@Named
@ViewScoped
public class PersonBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PersonService personService;

    private PersonDto person = new PersonDto();

    private List<PersonDto> persons;

    private List<RoleType> roleTypes = Arrays.asList(RoleType.values());


    public void init() {
        persons = personService.findAll();
    }


    public void save() {

        personService.createPerson(person);

        person = new PersonDto();

        persons = personService.findAll();
    }


    public void delete(Long id) {

        personService.delete(id);

        persons = personService.findAll();
    }


    public void edit(PersonDto selectedPerson) {

        this.person = selectedPerson;
    }


    public void update() {

        personService.update(person);

        person = new PersonDto();

        persons = personService.findAll();
    }


    public PersonDto getPerson() {
        return person;
    }


    public void setPerson(PersonDto person) {
        this.person = person;
    }


    public List<PersonDto> getPersons() {
        return persons;
    }


    public List<RoleType> getRoleTypes() {
        return roleTypes;
    }
}
