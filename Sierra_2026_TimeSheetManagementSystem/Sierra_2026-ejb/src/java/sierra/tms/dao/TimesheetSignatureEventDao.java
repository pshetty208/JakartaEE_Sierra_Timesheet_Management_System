package sierra.tms.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import sierra.tms.entities.TimesheetSignatureEventEntity;
import sierra.tms.utils.enums.TimesheetSignatureAction;

@LocalBean
@Stateless
public class TimesheetSignatureEventDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public void save(TimesheetSignatureEventEntity event) {
        em.persist(event);
    }

    public Set<Long> findTimesheetIdsWithLatestAction(
            Collection<Long> timesheetIds,
            TimesheetSignatureAction action) {
        if (timesheetIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(em.createQuery("""
                SELECT event.timesheet.id
                FROM TimesheetSignatureEventEntity event
                WHERE event.id IN (
                    SELECT MAX(latest.id)
                    FROM TimesheetSignatureEventEntity latest
                    WHERE latest.timesheet.id IN :timesheetIds
                    GROUP BY latest.timesheet.id
                )
                AND event.action = :action
                """, Long.class)
                .setParameter("timesheetIds", timesheetIds)
                .setParameter("action", action)
                .getResultList());
    }
}
