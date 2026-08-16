package com.jee.web;

import com.jee.dto.PersonDto;
import com.jee.services.PersonService;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 *
 * @author pranavsudhir
 */
@RequestScoped
@Named
public class CurrentPersonBean {

    @EJB
    private PersonService personService;

    private PersonDto currentPerson;

    public PersonDto getCurrentPerson() {
        if (currentPerson == null) {
            currentPerson = personService.getCurrentPerson();
        }
        return currentPerson;
    }
}
