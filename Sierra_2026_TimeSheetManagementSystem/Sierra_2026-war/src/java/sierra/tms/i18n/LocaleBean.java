package sierra.tms.i18n;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Locale;

@Named
@SessionScoped
public class LocaleBean implements Serializable {

    // Serialization of session-scoped bean.
    private static final long serialVersionUID = 1L;

    private Locale locale = Locale.ENGLISH;

    public Locale getLocale() {
        return locale;
    }

    public String getLanguage() {
        return locale.getLanguage();
    }

    public void setLanguage(String language) {
        if (Locale.GERMAN.getLanguage().equals(language)) {
            locale = Locale.GERMAN;
        } else {
            locale = Locale.ENGLISH;
        }
    }

    public void useEnglish() {
        locale = Locale.ENGLISH;
    }

    public void useGerman() {
        locale = Locale.GERMAN;
    }
}
