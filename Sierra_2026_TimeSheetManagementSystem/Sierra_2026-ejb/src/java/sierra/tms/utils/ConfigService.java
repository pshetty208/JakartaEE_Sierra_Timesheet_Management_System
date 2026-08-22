package sierra.tms.utils;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.util.Properties;
import sierra.tms.utils.enums.States;

@Singleton
@Startup
public class ConfigService {

    private static final String CONFIG_RESOURCE = "sierra.properties";

    private Properties properties;

    @PostConstruct
    private void init() {
        properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing config file on classpath: " + CONFIG_RESOURCE);
            }
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + CONFIG_RESOURCE, e);
        }
    }

    public int getMaxHoursPerWeek() { 
        return getInt("tss.contract.max-hours-per-week"); 
    }
    
    public int getDefaultWorkingDaysPerWeek() { 
        return getInt("tss.contract.default-working-days-per-week"); 
    }
    
    public int getDefaultVacationDaysPerYear() { 
        return getInt("tss.contract.default-vacation-days-per-year"); 
    }
    
    public int getDefaultArchiveDurationMonths() { 
        return getInt("tss.contract.default-archive-duration-months"); 
    }

    public States getDefaultHolidayState() { 
        return States.valueOf(getString("tss.holiday.default-state")); 
    }
    
    public int getHolidayMinYear() { 
        return getInt("tss.holiday.min-year"); 
    }
    
    public int getHolidayMaxYear() { 
        return getInt("tss.holiday.max-year"); 
    }

    public ZoneId getTimeZone() { 
        return ZoneId.of(getString("tss.timezone")); 
    }

    public String getReminderSenderEmail() { 
        return getString("tss.reminder.sender-email"); 
    }
    
    public String getReminderEmailSubject() { 
        return getString("tss.reminder.email-subject"); 
    }

    private String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing config key: " + key);
        }
        return value;
    }

    private int getInt(String key) {
        return Integer.parseInt(getString(key));
    }
}
