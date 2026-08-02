package com.jee.dao;

import com.jee.entities.PersonEntity;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author pranavsudhir
 */
@LocalBean
@Stateless
public class PersonDao {

    @PersistenceContext(unitName = "Sierra-pu")
    private EntityManager em;

    public PersonEntity findByEmailAddress(String emailAddress) {
        try {
            return em.createQuery(
                    "SELECT person FROM PersonEntity person "
                    + "WHERE person.emailAddress = :emailAddress",
                    PersonEntity.class)
                    .setParameter("emailAddress", emailAddress)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
