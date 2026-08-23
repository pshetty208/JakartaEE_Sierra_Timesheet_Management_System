package sierra.tms.exceptions;

public class BusinessRuleViolationException extends TmsExceptionHandler {
    public BusinessRuleViolationException(String messageKey, Object... args) {
        super("BUSINESS_RULE_VIOLATION", messageKey, args);
    }
}
