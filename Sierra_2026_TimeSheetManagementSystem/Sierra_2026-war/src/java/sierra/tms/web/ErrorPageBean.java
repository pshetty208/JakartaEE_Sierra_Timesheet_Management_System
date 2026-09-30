package sierra.tms.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named
@RequestScoped
public class ErrorPageBean {

    private static final Logger LOGGER =
            Logger.getLogger(ErrorPageBean.class.getName());

    private String incidentId;

    @PostConstruct
    public void init() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String> parameters = context.getExternalContext()
                .getRequestParameterMap();
        incidentId = parameters.get("incident");

        if (incidentId == null || incidentId.isBlank()) {
            incidentId = UUID.randomUUID().toString();
            Map<String, Object> requestMap = context.getExternalContext()
                    .getRequestMap();
            Throwable exception = (Throwable) requestMap.get(
                    "jakarta.servlet.error.exception");
            Object status = requestMap.get("jakarta.servlet.error.status_code");
            String message = "HTTP request failed: status=" + status
                    + ", incident_id=" + incidentId;
            if (exception == null) {
                LOGGER.log(Level.WARNING, message);
            } else {
                LOGGER.log(Level.SEVERE, message, exception);
            }
        }
    }

    public String getIncidentId() {
        return incidentId;
    }
}
