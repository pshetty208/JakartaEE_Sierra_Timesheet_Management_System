package sierra.tms.exceptions;
// NOTE: Might not be the best decision, to create a new exception class for this, but this could be modelled into a generic case
public class TimesheetEntryOverlapException extends RuntimeException {

    public TimesheetEntryOverlapException() {
        super("This entry overlaps time already reported for that date.");
    }
}
