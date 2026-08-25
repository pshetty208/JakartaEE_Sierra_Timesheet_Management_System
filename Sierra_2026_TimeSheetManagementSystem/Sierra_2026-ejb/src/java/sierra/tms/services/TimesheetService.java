package sierra.tms.services;

import sierra.tms.dto.TimesheetDto;
import sierra.tms.dto.TimesheetEntryDto;
import jakarta.ejb.Remote;
import java.util.List;

@Remote
public interface TimesheetService {

//    public Long save(TimesheetDto timesheet);

    public TimesheetDto findById(Long id);

    public List<TimesheetDto> findAll();

    public List<TimesheetDto> findByContractId(Long contractId);

    public List<TimesheetDto> findForEntryManagement();

    public void update(TimesheetDto timesheet);

    public void delete(Long id);

    public Long addEntry(Long timesheetId, TimesheetEntryDto entry);

    public void updateEntry(TimesheetEntryDto entry);

    public void deleteEntry(Long entryId);
    
    TimesheetDto getForPrinting(Long id);
    
    void archiveTimesheet(Long id);
    
    void signTimesheet(Long timesheetId);
        
    void signAsSupervisor(Long timesheetId);
    
    void revokeSignature(Long timesheetId);
    
    void requestChanges(Long timesheetId);
    
}
