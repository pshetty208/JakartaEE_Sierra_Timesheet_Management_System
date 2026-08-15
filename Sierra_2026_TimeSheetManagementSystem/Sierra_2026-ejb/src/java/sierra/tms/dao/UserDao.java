package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import sierra.tms.entities.UserEntity;

@LocalBean
@Stateless
public class UserDao {
    
    @PersistenceContext(unitName = "Sierra-tms-pu")
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
