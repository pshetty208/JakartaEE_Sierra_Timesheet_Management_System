package sierra.tms.i18n;

import com.jee.web.CurrentPersonBean;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import sierra.tms.dto.PersonDto;
import sierra.tms.services.PersonService;
import sierra.tms.utils.enums.SupportedLanguage;

@Named
@SessionScoped
public class LocaleBean implements Serializable {

    // Serialization of session-scoped bean.
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(LocaleBean.class.getName());

    @EJB
    private PersonService personService;

    @Inject
    private CurrentPersonBean currentPersonBean;

    private Locale locale = Locale.ENGLISH;
    private String loadedUserEmail;

    public Locale getLocale() {
        loadPreferenceIfAvailable();
        return locale;
    }

    public String getLanguage() {
        return getLocale().getLanguage();
    }

    public void setLanguage(String language) {
        loadPreferenceIfAvailable();

        String languageCode = LanguageResolver.normalize(language);
        locale = LanguageResolver.resolveLocale(languageCode);

        if (loadedUserEmail != null) {
            try {
                personService.changeCurrentPersonPreferredLanguage(languageCode);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING,
                        "Could not persist the current user's language preference.",
                        exception);
            }
        }
    }

    public void useEnglish() {
        setLanguage(LanguageResolver.toCode(SupportedLanguage.ENGLISH));
    }

    public void useGerman() {
        setLanguage(LanguageResolver.toCode(SupportedLanguage.GERMAN));
    }

    /** Loads the preference once for the currently authenticated person. */
    private void loadPreferenceIfAvailable() {
        String authenticatedUserEmail =
                currentPersonBean.getAuthenticatedUserEmail();

        if (authenticatedUserEmail == null) {
            clearPreviousUserPreference();
            return;
        }

        if (loadedUserEmail != null
                && !authenticatedUserEmail.equals(loadedUserEmail)) {
            clearPreviousUserPreference();
        }

        if (authenticatedUserEmail.equals(loadedUserEmail)) {
            return;
        }

        PersonDto currentPerson;
        try {
            currentPerson = currentPersonBean.getCurrentPerson();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING,
                    "Could not load the current user's language preference.",
                    exception);
            return;
        }
        if (currentPerson == null) {
            return;
        }

        locale = LanguageResolver.resolveLocale(
                currentPerson.getPreferredLanguage());
        loadedUserEmail = authenticatedUserEmail;
    }

    private void clearPreviousUserPreference() {
        if (loadedUserEmail != null) {
            locale = Locale.ENGLISH;
            loadedUserEmail = null;
        }
    }

}
