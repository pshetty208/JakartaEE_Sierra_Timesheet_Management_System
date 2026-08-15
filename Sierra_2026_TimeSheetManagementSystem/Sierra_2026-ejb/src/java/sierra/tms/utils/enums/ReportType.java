package sierra.tms.utils.enums;

/**
 * Type of work reported in a single timesheet entry.
 *
 * Weekends and public holidays are NOT report types - they are derived from
 * the calendar when a timesheet is rendered, and are never stored as entries.
 *
 * @author pranavpanhale
 */
public enum ReportType {

    WORK,
    VACATION,
    SICK_LEAVE
}
