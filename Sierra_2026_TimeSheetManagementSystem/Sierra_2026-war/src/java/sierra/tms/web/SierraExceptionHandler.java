package sierra.tms.web;

import jakarta.faces.FacesException;
import jakarta.faces.context.ExceptionHandler;
import jakarta.faces.context.ExceptionHandlerWrapper;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.event.ExceptionQueuedEvent;
import jakarta.faces.event.ExceptionQueuedEventContext;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SierraExceptionHandler extends ExceptionHandlerWrapper {

    private static final Logger LOGGER =
            Logger.getLogger(SierraExceptionHandler.class.getName());

    public SierraExceptionHandler(ExceptionHandler wrapped) {
        super(wrapped);
    }

    @Override
    public void handle() throws FacesException {
        Iterator<ExceptionQueuedEvent> events =
                getUnhandledExceptionQueuedEvents().iterator();
        if (!events.hasNext()) {
            getWrapped().handle();
            return;
        }

        ExceptionQueuedEvent event = events.next();
        ExceptionQueuedEventContext eventContext =
                (ExceptionQueuedEventContext) event.getSource();
        Throwable exception = eventContext.getException();
        String incidentId = UUID.randomUUID().toString();

        LOGGER.log(Level.SEVERE,
                "Unhandled JSF exception: incident_id=" + incidentId,
                exception);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();
        events.remove();

        if (externalContext.isResponseCommitted()) {
            getWrapped().handle();
            return;
        }

        try {
            String encodedIncidentId = URLEncoder.encode(
                    incidentId, StandardCharsets.UTF_8);
            externalContext.redirect(externalContext.getRequestContextPath()
                    + "/faces/error.xhtml?incident=" + encodedIncidentId);
            facesContext.responseComplete();
        } catch (IOException redirectFailure) {
            LOGGER.log(Level.SEVERE,
                    "Could not display the error page: incident_id=" + incidentId,
                    redirectFailure);
            throw new FacesException(redirectFailure);
        }
    }
}
