package sierra.tms.exceptions;

public class ValidationException extends TmsExceptionHandler {

    public ValidationException(String code, String messageKey, Object... args) {
        super(code, messageKey, args);
    }
}