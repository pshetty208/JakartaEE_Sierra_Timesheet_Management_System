package sierra.tms.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import sierra.tms.dto.TimesheetDto;
import sierra.tms.utils.enums.TimeSheetStatus;

@Named
@ApplicationScoped
public class DataTableExampleBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<TimesheetDto> timesheets = List.of(
            new TimesheetDto(
                    1L,
                    101L,
                    TimeSheetStatus.IN_PROGRESS,
                    LocalDate.of(2026, 8, 17),
                    LocalDate.of(2026, 8, 23),
                    null,
                    null,
                    List.of()),
            new TimesheetDto(
                    2L,
                    102L,
                    TimeSheetStatus.SIGNED_BY_EMPLOYEE,
                    LocalDate.of(2026, 8, 1),
                    LocalDate.of(2026, 8, 31),
                    LocalDate.of(2026, 8, 31),
                    null,
                    List.of()),
            new TimesheetDto(
                    3L,
                    103L,
                    TimeSheetStatus.SIGNED_BY_SUPERVISOR,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 8, 1),
                    List.of())
    );

    public List<TimesheetDto> getTimesheets() {
        return timesheets;
    }
}
