package sierra.tms.i18n;

import java.util.Locale;
import sierra.tms.utils.enums.SupportedLanguage;

/** Converts persisted language codes into supported application languages. */
public final class LanguageResolver {

    public static final String DEFAULT_LANGUAGE_CODE = "en";

    private static final String GERMAN_LANGUAGE_CODE = "de";

    private LanguageResolver() {
    }

    /** Validates and standardizes a code before it is persisted. */
    public static String normalize(String languageCode) {
        String normalizedCode = normalizeCode(languageCode);

        if (DEFAULT_LANGUAGE_CODE.equals(normalizedCode)
                || GERMAN_LANGUAGE_CODE.equals(normalizedCode)) {
            return normalizedCode;
        }

        throw new IllegalArgumentException("Unsupported language: " + languageCode);
    }

    /** Uses English when an existing preference is missing or unsupported. */
    public static Locale resolveLocale(String languageCode) {
        return toLocale(resolveOrDefault(languageCode));
    }

    /** Converts a domain value to the stable code stored in the database. */
    public static String toCode(SupportedLanguage language) {
        return switch (language) {
            case GERMAN -> GERMAN_LANGUAGE_CODE;
            case ENGLISH -> DEFAULT_LANGUAGE_CODE;
        };
    }

    private static Locale toLocale(SupportedLanguage language) {
        return switch (language) {
            case GERMAN -> Locale.GERMAN;
            case ENGLISH -> Locale.ENGLISH;
        };
    }

    private static SupportedLanguage resolveOrDefault(String languageCode) {
        return switch (normalizeCode(languageCode)) {
            case GERMAN_LANGUAGE_CODE -> SupportedLanguage.GERMAN;
            default -> SupportedLanguage.ENGLISH;
        };
    }

    private static String normalizeCode(String languageCode) {
        return languageCode == null
                ? ""
                : languageCode.trim().toLowerCase(Locale.ROOT);
    }
}
