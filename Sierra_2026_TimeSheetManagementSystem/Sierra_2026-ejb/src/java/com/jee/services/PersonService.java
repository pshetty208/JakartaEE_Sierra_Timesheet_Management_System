package com.jee.services;

import com.jee.dto.PersonDto;
import jakarta.ejb.Remote;

/**
 *
 * @author pranavsudhir
 */
@Remote
public interface PersonService {

    public PersonDto findByEmailAddress(String emailAddress);

    public PersonDto getCurrentPerson();
}
