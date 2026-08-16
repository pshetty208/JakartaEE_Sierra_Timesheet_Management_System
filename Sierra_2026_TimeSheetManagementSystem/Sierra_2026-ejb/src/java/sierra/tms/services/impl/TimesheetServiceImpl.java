package sierra.tms.services.impl;

import sierra.tms.dao.ContractDao;
import sierra.tms.dao.TimesheetDao;
import sierra.tms.dao.TimesheetEntryDao;
import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import sierra.tms.entities.ContractEntity;
import sierra.tms.entities.TimesheetEntity;
import sierra.tms.entities.TimesheetEntryEntity;
import sierra.tms.services.TimesheetService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;

@Stateless
@RolesAllowed({"EMPLOYEE", "SUPERVISOR", "ASSISTANT", "SECRETARY"})
public class TimesheetServiceImpl implements TimesheetService {

    @EJB
    private TimesheetDao dao;

    @EJB
    private TimesheetEntryDao entryDao;

    @EJB
    private ContractDao contractDao;

    @Override
    public Long save(TimesheetDto timesheet) {

        ContractEntity contract = contractDao.findById(timesheet.getContractId());

        if (contract == null) {
            return null;
        }

        TimesheetEntity entity = new TimesheetEntity();
        entity.setContract(contract);
        entity.setStartDate(timesheet.getStartDate());
        entity.setEndDate(timesheet.getEndDate());

        if (timesheet.getStatus() != null) {
            entity.setStatus(timesheet.getStatus());
        }

        dao.save(entity);

        return entity.getId();
    }

    @Override
    public TimesheetDto getById(Long id) {

        TimesheetEntity entity = dao.findById(id);

        return entity == null ? null : createDTO(entity);
    }

    @Override
    public List<TimesheetDto> getAll() {
        return dao.findAll()
                .stream()
                .map(this::createDTO)
                .toList();
    }

    @Override
    public List<TimesheetDto> getByContract(Long contractId) {
        return dao.findByContractId(contractId)
                .stream()
                .map(this::createDTO)
                .toList();
    }

    @Override
    public void update(TimesheetDto timesheet) {

        TimesheetEntity entity = dao.findById(timesheet.getId());

        if (entity == null) {
            return;
        }

        entity.setStartDate(timesheet.getStartDate());
        entity.setEndDate(timesheet.getEndDate());
        entity.setStatus(timesheet.getStatus());
        entity.setSignedByEmployee(timesheet.getSignedByEmployee());
        entity.setSignedBySupervisor(timesheet.getSignedBySupervisor());

        dao.update(entity);
    }

    @Override
    public void delete(Long id) {
        // TODO(TS5/TS6): refuse deletion when the timesheet is SIGNED_BY_EMPLOYEE
        // or SIGNED_BY_SUPERVISOR.
        dao.delete(id);
    }

    @Override
    public Long addEntry(Long timesheetId, TimesheetEntryDto entry) {

        TimesheetEntity timesheet = dao.findById(timesheetId);

        if (timesheet == null) {
            return null;
        }

        // TODO(TS2): only allow this when timesheet is IN_PROGRESS and its
        // contract is STARTED. Needs the contract slice.
        // TODO(TS3): reject if total VACATION hours would exceed the contract's
        // vacation hours (CN4a).

        TimesheetEntryEntity entity = new TimesheetEntryEntity();
        entity.setType(entry.getType());
        entity.setDescription(entry.getDescription());
        entity.setEntryDate(entry.getEntryDate());
        entity.setStartTime(entry.getStartTime());
        entity.setEndTime(entry.getEndTime());

        timesheet.addEntry(entity);
        entryDao.save(entity);

        return entity.getId();
    }

    @Override
    public void updateEntry(TimesheetEntryDto entry) {

        TimesheetEntryEntity entity = entryDao.findById(entry.getId());

        if (entity == null) {
            return;
        }

        // TODO(TS2): same status gate as addEntry.

        entity.setType(entry.getType());
        entity.setDescription(entry.getDescription());
        entity.setEntryDate(entry.getEntryDate());
        entity.setStartTime(entry.getStartTime());
        entity.setEndTime(entry.getEndTime());

        entryDao.update(entity);
    }

    @Override
    public void deleteEntry(Long entryId) {
        // TODO(TS2): same status gate as addEntry.
        entryDao.delete(entryId);
    }

    private TimesheetDto createDTO(TimesheetEntity entity) {
        return new TimesheetDto(
                entity.getId(),
                entity.getContract() == null ? null : entity.getContract().getId(),
                entity.getStatus(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getSignedByEmployee(),
                entity.getSignedBySupervisor(),
                entity.getEntries()
                        .stream()
                        .map(this::createDTO)
                        .toList()
        );
    }

    private TimesheetEntryDto createDTO(TimesheetEntryEntity entity) {
        return new TimesheetEntryDto(
                entity.getId(),
                entity.getTimesheet() == null ? null : entity.getTimesheet().getId(),
                entity.getType(),
                entity.getDescription(),
                entity.getEntryDate(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getHours()
        );
    }
}
