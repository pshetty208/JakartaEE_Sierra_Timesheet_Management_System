package sierra.tms.service;

import jakarta.ejb.Remote;
import java.time.LocalDate;
import java.util.Set;

@Remote
public interface HolidayService {

    Set<LocalDate> getPublicHolidays(int year);

    boolean isPublicHoliday(LocalDate date);

}