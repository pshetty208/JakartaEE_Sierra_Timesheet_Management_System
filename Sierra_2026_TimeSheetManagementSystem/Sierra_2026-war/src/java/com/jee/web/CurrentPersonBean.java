package com.jee.web;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.FacesException;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import sierra.tms.dto.PersonDto;
import sierra.tms.services.PersonService;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author pranavsudhir
 */
@RequestScoped
@Named
public class CurrentPersonBean {

    private static final Logger LOGGER = Logger.getLogger(CurrentPersonBean.class.getName());

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
            try {
                currentPerson = personService.getCurrentPerson();
            } catch (RuntimeException exception) {
                LOGGER.log(Level.SEVERE, "Unable to load the current user.", exception);
                return null;
            }
        }
        return currentPerson;
    }

    public String getDisplayName() {
        PersonDto person = getCurrentPerson();
        if (person == null) {
            return getAuthenticatedUserEmail();
        }

        String firstName = person.getFirstName() == null ? "" : person.getFirstName().trim();
        String lastName = person.getLastName() == null ? "" : person.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isEmpty() ? person.getEmailAddress() : fullName;
    }

    public String getInitials() {
        PersonDto person = getCurrentPerson();
        if (person != null) {
            String first = firstLetter(person.getFirstName());
            String last = firstLetter(person.getLastName());
            if (!first.isEmpty() || !last.isEmpty()) {
                return (first + last).toUpperCase(Locale.ROOT);
            }
        }

        String email = getAuthenticatedUserEmail();
        return email == null || email.isBlank()
                ? "U"
                : email.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public void logout() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context
                .getExternalContext()
                .getRequest();

        try {
            request.logout();
            context.getExternalContext().invalidateSession();
            context.getExternalContext().redirect(
                    context.getExternalContext().getRequestContextPath()
                    + "/faces/login.xhtml");
            context.responseComplete();
        } catch (ServletException | IOException exception) {
            LOGGER.log(Level.SEVERE, "Unable to log out the current user.", exception);
            throw new FacesException("Unable to log out the current user.", exception);
        }
    }

    private String firstLetter(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1);
    }
}
