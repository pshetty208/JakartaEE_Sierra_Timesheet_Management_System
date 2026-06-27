package sierra.tms.web;

import sierra.tms.dto.UserDto;
import sierra.tms.service.UserService;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author prajnashetty
 */
@Named
@ViewScoped
public class UserBean implements Serializable {
    
    @EJB
    private UserService service;

    private String name;

    public void save() {

        service.save(name);

        name = "";
    }

    public List<UserDto> getPeople() {

        return service.getAll();

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
