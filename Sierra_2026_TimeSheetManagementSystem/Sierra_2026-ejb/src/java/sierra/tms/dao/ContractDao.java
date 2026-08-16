package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import sierra.tms.entities.ContractEntity;

@LocalBean
@Stateless
public class ContractDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public void save(ContractEntity contract) {
        em.persist(contract);
    }
    
    public ContractEntity findById(Long id) {
        return em.find(ContractEntity.class, id);
    }
    
    public List<ContractEntity> findByEmployee(Long employeeId) {
        return em.createQuery(
                "SELECT c FROM ContractEntity c WHERE c.employee.id = :employeeId",
                ContractEntity.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }   

    public List<ContractEntity> findAll() {

    return em.createQuery(
            "SELECT c FROM ContractEntity c",
            ContractEntity.class)
            .getResultList();
    }
    
    public ContractEntity update(ContractEntity contract) {
        return em.merge(contract);
    }

    public void delete(ContractEntity contract) {
        em.remove(em.contains(contract) ? contract : em.merge(contract));
    }

}
