package sierra.tms.services;

import jakarta.ejb.Local;
import java.time.LocalDate;
import sierra.tms.utils.enums.States;

@Local
public interface HolidayService {

    boolean isPublicHoliday(LocalDate date);
    
    boolean isPublicHoliday(LocalDate date, States state);

}