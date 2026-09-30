package sierra.tms.dao;

import sierra.tms.entities.TimesheetEntity;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import sierra.tms.utils.enums.TimeSheetStatus;

@LocalBean
@Stateless
public class TimesheetDao {

    @PersistenceContext(unitName = "Sierra-tms-pu")
    private EntityManager em;

    public void save(TimesheetEntity timesheet) {
        em.persist(timesheet);
        // IDENTITY keys are only assigned once the INSERT runs, so flush here
        // to make the generated id readable by the caller.
        em.flush();
    }

    public TimesheetEntity findById(Long id) {
        return em.find(TimesheetEntity.class, id);
    }

    public List<TimesheetEntity> findAll() {
        return em.createQuery(
                "SELECT timesheet FROM TimesheetEntity timesheet",
                TimesheetEntity.class)
                .getResultList();
    }

    public List<TimesheetEntity> findByContractId(Long contractId) {
        return em.createQuery(
                "SELECT timesheet FROM TimesheetEntity timesheet"
                + " WHERE timesheet.contract.id = :contractId"
                + " ORDER BY timesheet.startDate",
                TimesheetEntity.class)
                .setParameter("contractId", contractId)
                .getResultList();
    }
    
    public List<TimesheetEntity> findByStatus(TimeSheetStatus status) {
        return em.createQuery(
                "SELECT t FROM TimesheetEntity t WHERE t.status = :status",
                TimesheetEntity.class)
                .setParameter("status", status)
                .getResultList();
    }
    
    /** Timesheets the supervisor has signed (SIGNED_BY_SUPERVISOR or ARCHIVED). */
    public List<TimesheetEntity> findSignedBySupervisor() {
        return em.createQuery(
                "SELECT t FROM TimesheetEntity t WHERE t.signedBySupervisor IS NOT NULL",
                TimesheetEntity.class)
                .getResultList();
    }

    public void delete(TimesheetEntity timesheet) {
        em.remove(em.contains(timesheet) ? timesheet : em.merge(timesheet));
    }

    public TimesheetEntity update(TimesheetEntity timesheet) {
        return em.merge(timesheet);
    }

    public void delete(Long id) {
        TimesheetEntity timesheet = em.find(TimesheetEntity.class, id);
        if (timesheet != null) {
            em.remove(timesheet);
        }
    }
}
