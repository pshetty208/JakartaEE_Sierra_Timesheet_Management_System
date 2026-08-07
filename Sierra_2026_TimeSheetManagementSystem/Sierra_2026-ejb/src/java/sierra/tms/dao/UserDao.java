package com.jee.dao;

import com.jee.dto.UserDto;
import com.jee.entities.UserEntity;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@LocalBean
@Stateless
public class UserDao {
    
    @PersistenceContext(unitName = "Sierra-pu")
    private EntityManager em;

    public void save(UserEntity person) {
        em.persist(person);
    }

    public List<UserEntity> findAll() {
        return em.createQuery(
                "SELECT user FROM UserEntity user",
                UserEntity.class)
                .getResultList();
    }
    
}
