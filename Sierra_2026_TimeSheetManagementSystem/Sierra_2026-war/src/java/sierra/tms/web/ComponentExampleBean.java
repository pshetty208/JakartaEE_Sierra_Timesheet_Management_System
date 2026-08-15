package sierra.tms.web;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import sierra.tms.dto.UserDto;

@Named
@ApplicationScoped
public class ComponentExampleBean implements Serializable {

    private final List<UserDto> users = List.of(
            new UserDto(1L, "Prajna Shetty"),
            new UserDto(2L, "Manas Gupta"),
            new UserDto(3L, "Sierra Admin"),
            new UserDto(4L, "Timesheet Reviewer")
    );

    public List<UserDto> getUsers() {
        return users;
    }
}
