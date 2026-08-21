package sierra.tms.config;

import java.time.ZoneId;

/** Values shared across the application runtime. */
public final class ApplicationConfig {

    public static final String TIME_ZONE_ID = "Europe/Berlin";
    public static final ZoneId TIME_ZONE = ZoneId.of(TIME_ZONE_ID);

    private ApplicationConfig() {
        // Utility class; prevent instantiation.
    }
}
