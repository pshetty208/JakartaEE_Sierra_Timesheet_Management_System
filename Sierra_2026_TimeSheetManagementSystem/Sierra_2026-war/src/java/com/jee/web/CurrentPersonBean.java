package com.jee.web;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import sierra.tms.dto.PersonDto;
import sierra.tms.services.PersonService;

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
