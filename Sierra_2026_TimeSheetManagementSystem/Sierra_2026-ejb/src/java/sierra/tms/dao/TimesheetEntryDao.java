package sierra.tms.dao;

import sierra.tms.entities.TimesheetEntryEntity;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

/**
 *
 * @author pranavpanhale
 */
@LocalBean
@Stateless
public class TimesheetEntryDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public void save(TimesheetEntryEntity entry) {
        em.persist(entry);
        // IDENTITY keys are only assigned once the INSERT runs, so flush here
        // to make the generated id readable by the caller.
        em.flush();
    }

    public TimesheetEntryEntity findById(Long id) {
        return em.find(TimesheetEntryEntity.class, id);
    }

    public List<TimesheetEntryEntity> findByTimesheetId(Long timesheetId) {
        // Alias is "te", not "entry": ENTRY is a reserved JPQL keyword (ENTRY(m)
        // over a Map), so using it as an identification variable fails to parse.
        return em.createQuery(
                "SELECT te FROM TimesheetEntryEntity te"
                + " WHERE te.timesheet.id = :timesheetId"
                + " ORDER BY te.entryDate, te.startTime",
                TimesheetEntryEntity.class)
                .setParameter("timesheetId", timesheetId)
                .getResultList();
    }

    public TimesheetEntryEntity update(TimesheetEntryEntity entry) {
        return em.merge(entry);
    }

    public void delete(Long id) {
        TimesheetEntryEntity entry = em.find(TimesheetEntryEntity.class, id);
        if (entry != null) {
            em.remove(entry);
        }
    }
}
