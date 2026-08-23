package sierra.tms.web;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import sierra.tms.exceptions.TmsExceptionHandler;

public final class TmsExceptionMessages {

    private static final String BUNDLE = "sierra.tms.i18n.messages";

    private TmsExceptionMessages() {
    }

    public static void showError(Throwable throwable) {
        TmsExceptionHandler exception = findTmsException(throwable);

        if (exception == null) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            message("error.unexpected"),
                            null));
            return;
        }

        FacesContext.getCurrentInstance().addMessage(
                null,
                new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        message(exception.getMessageKey(),
                                exception.getMessageArguments()),
                        exception.getCode()));
    }

    private static TmsExceptionHandler findTmsException(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof TmsExceptionHandler tmsException) {
                return tmsException;
            }
            current = current.getCause();
        }

        return null;
    }

    private static String message(String key, Object... arguments) {
        Locale locale = FacesContext.getCurrentInstance()
                .getViewRoot()
                .getLocale();

        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, locale);
        String template = bundle.containsKey(key)
                ? bundle.getString(key)
                : bundle.getString("error.unexpected");

        return MessageFormat.format(template, arguments);
    }
}