package com.jee.web;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.security.Principal;
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

    public boolean isAuthenticated() {
        return getAuthenticatedUserEmail() != null;
    }

    public String getAuthenticatedUserEmail() {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context == null) {
            return null;
        }

        Principal authenticatedUser = context
                .getExternalContext()
                .getUserPrincipal();

        return authenticatedUser == null
                ? null
                : authenticatedUser.getName();
    }

    public PersonDto getCurrentPerson() {
        if (!isAuthenticated()) {
            return null;
        }

        if (currentPerson == null) {
            currentPerson = personService.getCurrentPerson();
        }
        return currentPerson;
    }
}
