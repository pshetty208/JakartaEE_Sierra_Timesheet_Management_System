package sierra.tms.services.impl;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import sierra.tms.services.HolidayService;
import sierra.tms.utils.ConfigService;
import sierra.tms.utils.enums.States;
import static sierra.tms.utils.enums.States.RHINELAND_PALATINATE;

@Stateless
public class HolidayServiceImpl implements HolidayService {
    
    @EJB
    private ConfigService configService;

    @Override
    public boolean isPublicHoliday(LocalDate date) {
        return isPublicHoliday(date, configService.getDefaultHolidayState());
    }        

    @Override
    public boolean isPublicHoliday(LocalDate date, States state) {
        if (date == null) {
            throw new IllegalArgumentException("Date must not be null.");
        }

        if (state == null) {
            throw new IllegalArgumentException("Federal state must not be null.");
        }

        int year = date.getYear();
        if (year < configService.getHolidayMinYear() || year > configService.getHolidayMaxYear() ) {
            throw new IllegalArgumentException("Public holidays are supported from " + configService.getHolidayMinYear() + " to " + configService.getHolidayMaxYear() + ".");
        }

        return holidaysFor(date.getYear(), state).contains(date);
    }

    private Set<LocalDate> holidaysFor(int year, States state) {
        LocalDate easterSunday = easterSunday(year);
        Set<LocalDate> holidays = new HashSet<>();
        
        // Nationwide public holidays
        holidays.add(LocalDate.of(year, 1, 1));       // New Year's Day
        holidays.add(easterSunday.minusDays(2));      // Good Friday
        holidays.add(easterSunday.plusDays(1));       // Easter Monday
        holidays.add(LocalDate.of(year, 5, 1));       // Labour Day
        holidays.add(easterSunday.plusDays(39));      // Ascension Day
        holidays.add(easterSunday.plusDays(50));      // Whit Monday
        holidays.add(LocalDate.of(year, 10, 3));      // German Unity Day
        holidays.add(LocalDate.of(year, 12, 25));     // Christmas Day
        holidays.add(LocalDate.of(year, 12, 26));     // Boxing Day

        // State-specific public holidays
        stateHolidays(holidays, year, state, easterSunday);

        return holidays;
    }
        
    private void stateHolidays(Set<LocalDate> holidays, int year, States state, LocalDate easterSunday) {
        
         switch (state) {

            case BADEN_WUERTTEMBERG:
                holidays.add(LocalDate.of(year, 1, 6));   // Epiphany
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                holidays.add(LocalDate.of(year, 11, 1));  // All Saints' Day
                break;

            case BAVARIA:
                holidays.add(LocalDate.of(year, 1, 6));   // Epiphany
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                holidays.add(LocalDate.of(year, 11, 1));  // All Saints' Day
                holidays.add(LocalDate.of(year, 8, 15));  // Assumption Day
                break;

            case BERLIN:
                holidays.add(LocalDate.of(year, 3, 8));   // International Women's Day
                break;

            case BRANDENBURG:
                holidays.add(easterSunday.plusDays(49));  // Whit Sunday
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;

            case BREMEN:
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;

            case HAMBURG:
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;
                
            case HESSE:
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                break;

            case MECKLENBURG_VORPOMMERN:
                holidays.add(LocalDate.of(year, 3, 8));   // International Women's Day
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;

            case LOWER_SAXONY:
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;

            case NORTH_RHINE_WESTPHALIA:
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                holidays.add(LocalDate.of(year, 11, 1));  // All Saints' Day
                break;

            case RHINELAND_PALATINATE:
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                holidays.add(LocalDate.of(year, 11, 1));  // All Saints' Day
                break;

            case SAARLAND:
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                holidays.add(LocalDate.of(year, 8, 15));  // Assumption Day
                holidays.add(LocalDate.of(year, 11, 1));  // All Saints' Day
                break;

            case SAXONY:
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                holidays.add(easterSunday.plusDays(60));  // Corpus Christi
                break;
                
            case SAXONY_ANHALT:
                holidays.add(LocalDate.of(year, 1, 6));   // Epiphany
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;

            case SCHLESWIG_HOLSTEIN:
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;

            case THURINGIA:
                holidays.add(LocalDate.of(year, 9, 20));  // World Children's Day
                holidays.add(LocalDate.of(year, 10, 31)); // Reformation Day
                break;
        }
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