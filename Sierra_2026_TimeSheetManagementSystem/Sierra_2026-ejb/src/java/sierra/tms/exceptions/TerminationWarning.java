package sierra.tms.exceptions;

import jakarta.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class TerminationWarning extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TerminationWarning(String message) {
        super(message);
    }
}
