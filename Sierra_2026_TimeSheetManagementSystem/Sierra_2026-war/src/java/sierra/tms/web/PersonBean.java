package sierra.tms.web;

import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import sierra.tms.dto.PersonDto;
import sierra.tms.exceptions.RuleViolation;
import sierra.tms.i18n.UiMessages;
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

    private RoleType selectedRole;

    private String initialPassword;


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
            person.setRoles(List.of(selectedRole));
            personService.createPerson(person, initialPassword);
            String emailAddress = person.getEmailAddress().trim().toLowerCase(Locale.ROOT);
            person = new PersonDto();
            selectedRole = null;
            initialPassword = null;
            persons = personService.findAll();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            UiMessages.get("person.create.success", emailAddress), null));
        } catch (RuleViolation violation) {
            WebExceptionHandler.showRuleViolation(violation);
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


    public LocalDate getLatestDateOfBirth() {
        return LocalDate.now().minusYears(PersonService.MINIMUM_AGE);
    }


    public String roleLabel(RoleType role) {
        return UiMessages.get("role." + role.name());
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


    public RoleType getSelectedRole() {
        return selectedRole;
    }


    public void setSelectedRole(RoleType selectedRole) {
        this.selectedRole = selectedRole;
    }


    public String getInitialPassword() {
        return initialPassword;
    }


    public void setInitialPassword(String initialPassword) {
        this.initialPassword = initialPassword;
    }
}
