package sierra.tms.services;

import jakarta.ejb.Remote;
import java.time.LocalDate;

@Remote
public interface HolidayService {

    boolean isPublicHoliday(LocalDate date);

}