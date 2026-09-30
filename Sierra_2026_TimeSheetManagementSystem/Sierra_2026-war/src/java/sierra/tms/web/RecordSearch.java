package sierra.tms.web;

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import sierra.tms.dto.PersonDto;

/**
 * Case-insensitive text matching for the administrator search on the contract and
 * timesheet pages. A record matches when any of its searchable values contains the term.
 */
final class RecordSearch {

    private RecordSearch() {
    }

    static boolean isBlank(String term) {
        return term == null || term.isBlank();
    }

    static boolean matches(String term, String... values) {
        if (isBlank(term)) {
            return true;
        }
        String needle = normalize(term);
        return Stream.of(values)
                .filter(Objects::nonNull)
                .map(RecordSearch::normalize)
                .anyMatch(value -> value.contains(needle));
    }

    /** User name (login e-mail) and full name of a person, for matching. */
    static String[] personValues(PersonDto person) {
        if (person == null) {
            return new String[0];
        }
        String fullName = ((person.getFirstName() == null ? "" : person.getFirstName()) + " "
                + (person.getLastName() == null ? "" : person.getLastName())).trim();
        return new String[]{person.getEmailAddress(), fullName};
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
