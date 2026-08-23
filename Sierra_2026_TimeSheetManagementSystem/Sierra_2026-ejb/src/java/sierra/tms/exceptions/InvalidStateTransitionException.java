package sierra.tms.exceptions;

public class InvalidStateTransitionException extends TmsExceptionHandler {
    public InvalidStateTransitionException(String messageKey, Object... args) {
        super("INVALID_STATE_TRANSITION", messageKey, args);
    }
}