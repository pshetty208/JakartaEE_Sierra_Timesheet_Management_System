package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import sierra.tms.entities.TimesheetEntity;

@Stateless
@LocalBean
public class TimesheetDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public TimesheetEntity findById(Long id) {
        return em.find(TimesheetEntity.class, id);
    }
    
    public List<TimesheetEntity> findByContract(Long contractId) {

        return em.createQuery(
                "SELECT t FROM TimesheetEntity t " +
                "WHERE t.contract.id = :contractId",
                TimesheetEntity.class)
                .setParameter("contractId", contractId)
                .getResultList();
    }

}
