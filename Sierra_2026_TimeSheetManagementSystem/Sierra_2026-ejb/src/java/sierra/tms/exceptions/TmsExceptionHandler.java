package sierra.tms.exceptions;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = true, inherited = true)
public abstract class TmsExceptionHandler extends RuntimeException {

    private final String code;
    private final String messageKey;
    private final Object[] messageArguments;

    protected TmsExceptionHandler(String code, String messageKey, Object... messageArguments) {
        super(messageKey);
        this.code = code;
        this.messageKey = messageKey;
        this.messageArguments = messageArguments == null ? new Object[0] : messageArguments.clone();
    }

    public String getCode() {
        return code;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getMessageArguments() {
        return messageArguments.clone();
    }
}