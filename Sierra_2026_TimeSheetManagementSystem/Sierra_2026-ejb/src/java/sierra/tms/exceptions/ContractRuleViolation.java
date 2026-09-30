package sierra.tms.exceptions;

import jakarta.ejb.ApplicationException;
import java.util.Arrays;

@ApplicationException(rollback = true)
public class ContractRuleViolation extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String messageKey;
    private final Object[] arguments;

    public ContractRuleViolation(String messageKey, Object... arguments) {
        super(messageKey + " " + Arrays.toString(arguments));
        this.messageKey = messageKey;
        this.arguments = arguments;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArguments() {
        return arguments;
    }
}
