package sierra.tms.services;

import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import jakarta.ejb.Remote;
import java.util.List;

/**
 *
 * @author pranavpanhale
 */
@Remote
public interface TimesheetService {

    public Long save(TimesheetDto timesheet);

    public TimesheetDto getById(Long id);

    public List<TimesheetDto> getAll();

    public List<TimesheetDto> getByContract(Long contractId);

    public void update(TimesheetDto timesheet);

    public void delete(Long id);

    public Long addEntry(Long timesheetId, TimesheetEntryDto entry);

    public void updateEntry(TimesheetEntryDto entry);

    public void deleteEntry(Long entryId);
}
