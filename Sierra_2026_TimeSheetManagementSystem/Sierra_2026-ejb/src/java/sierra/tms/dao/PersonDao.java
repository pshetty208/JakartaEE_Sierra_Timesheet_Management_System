package sierra.tms.dao;

import sierra.tms.entities.PersonEntity;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@LocalBean
@Stateless
public class PersonDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public void save(PersonEntity person) {
        em.persist(person);
    }
    
    public PersonEntity findById(Long id) {
        return em.find(PersonEntity.class, id);
    }
    
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

    public List<PersonEntity> findAll() {
        return em.createQuery("SELECT person FROM PersonEntity person",
                PersonEntity.class)
                .getResultList();
    }
    
    public PersonEntity update(PersonEntity person) {
        return em.merge(person);
    }

    public void delete(PersonEntity person) {
        em.remove(em.contains(person) ? person : em.merge(person));
    }

}
