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
        try {
            persons = personService.findAll();
        } catch (RuntimeException exception) {
            persons = List.of();
            WebExceptionHandler.handle(getClass(), "load people",
                    "common.error.loadFailed", exception);
        }
    }


    public void save() {

        try {
            personService.createPerson(person);
            person = new PersonDto();
            persons = personService.findAll();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "create person",
                    "common.error.operationFailed", exception);
        }
    }


    public void delete(Long id) {

        try {
            personService.delete(id);
            persons = personService.findAll();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "delete person",
                    "common.error.operationFailed", exception);
        }
    }


    public void edit(PersonDto selectedPerson) {

        this.person = selectedPerson;
    }


    public void update() {

        try {
            personService.update(person);
            person = new PersonDto();
            persons = personService.findAll();
        } catch (RuntimeException exception) {
            WebExceptionHandler.handle(getClass(), "update person",
                    "common.error.operationFailed", exception);
        }
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
