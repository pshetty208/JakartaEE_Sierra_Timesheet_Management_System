package sierra.tms.exceptions;

public class AccessDeniedException extends TmsExceptionHandler {
    public AccessDeniedException() {
        super("ACCESS_DENIED", "error.accessDenied");
    }
    public AccessDeniedException(String messageKey, Object... args) {
        super("ACCESS_DENIED", messageKey, args);
    }
}