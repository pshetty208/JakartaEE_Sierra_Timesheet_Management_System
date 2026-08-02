package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import sierra.tms.entities.TimesheetEntity;

/**
 *
 * @author prajnashetty
 */

@Stateless
@LocalBean
public class TimesheetDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public TimesheetEntity findById(Long id) {
        return em.find(TimesheetEntity.class, id);
    }

}
