package sierra.tms.i18n;

import jakarta.faces.context.FacesContext;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class UiMessages {

    private static final String BUNDLE_NAME = "sierra.tms.i18n.messages";

    private UiMessages() {
    }

    public static String get(String key, Object... arguments) {
        FacesContext context = FacesContext.getCurrentInstance();
        Locale locale = resolveLocale(context);
        String pattern = ResourceBundle.getBundle(BUNDLE_NAME, locale).getString(key);
        if (arguments.length == 0) {
            return pattern;
        }
        return new MessageFormat(pattern, locale).format(arguments);
    }

    private static Locale resolveLocale(FacesContext context) {
        if (context == null) {
            return Locale.ENGLISH;
        }

        Locale locale = context.getApplication().evaluateExpressionGet(
                context, "#{localeBean.locale}", Locale.class);
        if (locale != null) {
            return locale;
        }

        return context.getViewRoot() != null && context.getViewRoot().getLocale() != null
                ? context.getViewRoot().getLocale()
                : Locale.ENGLISH;
    }
}
