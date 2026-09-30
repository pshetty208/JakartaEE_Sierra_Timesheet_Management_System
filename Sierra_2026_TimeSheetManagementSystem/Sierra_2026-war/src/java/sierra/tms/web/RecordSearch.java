package sierra.tms.web;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import sierra.tms.dto.PersonDto;

/**
 * Case-insensitive text matching for the search on the contract and timesheet pages.
 * A record matches when any of its searchable values contains the term. Every role can
 * search, but only within the records it is already allowed to see.
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

    /**
     * Whether the caller may load the person directory (PersonService.findAll). Employees
     * may not, so their search uses the names already carried by their contracts.
     */
    static boolean canListPeople() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null) {
            return false;
        }
        ExternalContext external = context.getExternalContext();
        return external.isUserInRole("SUPERVISOR") || external.isUserInRole("ASSISTANT")
                || external.isUserInRole("SECRETARY") || external.isUserInRole("ADMIN");
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
