package com.jee.services.impl;

import com.jee.dao.PersonDao;
import com.jee.dto.PersonDto;
import com.jee.entities.PersonEntity;
import com.jee.services.PersonService;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 *
 * @author pranavsudhir
 */
@Stateless
public class PersonServiceImpl implements PersonService {

    @EJB
    private PersonDao dao;

    @Resource
    private SessionContext sessionContext;

    @Override
    public PersonDto findByEmailAddress(String emailAddress) {
        PersonEntity entity = dao.findByEmailAddress(emailAddress);
        return entity == null ? null : createDTO(entity);
    }

    @Override
    public PersonDto getCurrentPerson() {
        String emailAddress = sessionContext.getCallerPrincipal().getName();
        return findByEmailAddress(emailAddress);
    }

    private PersonDto createDTO(PersonEntity entity) {
        return new PersonDto(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getDateOfBirth(),
                entity.getEmailAddress(),
                entity.isConsent()
        );
    }
}
