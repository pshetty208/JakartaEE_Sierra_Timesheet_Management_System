package sierra.tms.web;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import sierra.tms.dto.PersonDto;

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

    static String[] personValues(PersonDto person) {
        if (person == null) {
            return new String[0];
        }
        String fullName = ((person.getFirstName() == null ? "" : person.getFirstName()) + " "
                + (person.getLastName() == null ? "" : person.getLastName())).trim();
        return new String[]{person.getEmailAddress(), fullName};
    }

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
