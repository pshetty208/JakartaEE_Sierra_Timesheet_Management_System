package com.jee.dao;

import com.jee.entities.ContractEntity;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 *
 * @author pranavsudhir
 */
@LocalBean
@Stateless
public class ContractDao {

    @PersistenceContext(unitName = "Sierra-pu")
    private EntityManager em;

    public ContractEntity findById(Long id) {
        return em.find(ContractEntity.class, id);
    }
}
