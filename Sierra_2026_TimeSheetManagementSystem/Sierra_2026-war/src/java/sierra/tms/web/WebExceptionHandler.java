package sierra.tms.web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.i18n.UiMessages;

final class WebExceptionHandler {

    private WebExceptionHandler() {
    }

    static void handle(Class<?> source, String operation, String messageKey,
            RuntimeException exception) {
        String incidentId = UUID.randomUUID().toString();
        Logger.getLogger(source.getName()).log(Level.SEVERE,
                "Operation failed: " + operation + ", incident_id=" + incidentId,
                exception);

        FacesContext context = FacesContext.getCurrentInstance();
        if (context != null) {
            context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    UiMessages.get(messageKey, incidentId), null));
        }
    }
}
