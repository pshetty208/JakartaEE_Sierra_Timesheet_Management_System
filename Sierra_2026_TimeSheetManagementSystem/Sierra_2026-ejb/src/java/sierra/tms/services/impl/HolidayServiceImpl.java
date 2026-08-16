package sierra.tms.services.impl;

import jakarta.ejb.Stateless;
import java.time.LocalDate;
import java.util.Set;
import sierra.tms.services.HolidayService;
import sierra.tms.utils.enums.States;
import static sierra.tms.utils.enums.States.RHINELAND_PALATINATE;

@Stateless
public class HolidayServiceImpl implements HolidayService {
        
    private static final States DEFAULT_STATE = States.RHINELAND_PALATINATE;    

    @Override
    public boolean isPublicHoliday(LocalDate date) {
        return isPublicHoliday(date, DEFAULT_STATE);
    }        

    @Override
    public boolean isPublicHoliday(LocalDate date, States state) {
        return holidaysFor(date.getYear()).contains(date) || stateHolidaysFor(date.getYear(), state).contains(date);
    }

    private Set<LocalDate> holidaysFor(int year) {
        LocalDate easterSunday = easterSunday(year);

        return Set.of(
                LocalDate.of(year, 1, 1),       // New Year
                easterSunday.minusDays(2),      // Good Friday
                easterSunday.plusDays(1),       // Easter Monday
                LocalDate.of(year, 5, 1),       // Labour Day
                easterSunday.plusDays(39),      // Ascension Day
                easterSunday.plusDays(50),      // Whit Monday
                easterSunday.plusDays(60),      // Corpus Christi
                LocalDate.of(year, 10, 3),      // German Unity Day
                LocalDate.of(year, 11, 1),      // All Saints' Day
                LocalDate.of(year, 12, 25),     // Christmas Day
                LocalDate.of(year, 12, 26)      // Boxing Day
        );
    }
        
    private Set<LocalDate> stateHolidaysFor(int year, States state) {
        LocalDate easterSunday = easterSunday(year);
        return switch (state) {
            case RHINELAND_PALATINATE -> Set.of(
                    easterSunday.plusDays(60),      // Corpus Christi
                    LocalDate.of(year, 11, 1)        // All Saints' Day
            );
        };
    }

    private LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }

}