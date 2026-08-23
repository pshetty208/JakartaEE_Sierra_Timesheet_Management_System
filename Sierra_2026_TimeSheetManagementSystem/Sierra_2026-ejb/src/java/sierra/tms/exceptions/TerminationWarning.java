package sierra.tms.exceptions;

public class TerminationWarning extends TmsExceptionHandler {
    public TerminationWarning(String messageKey, Object... args) {
        super("TERMINATION_WARNING", messageKey, args);
    }
}
