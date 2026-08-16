package sierra.tms.services;

import jakarta.ejb.Remote;
import java.util.List;
import sierra.tms.dto.UserDto;

@Remote
public interface UserService {
    
    public void save(String name);

    public List<UserDto> getAll();
}
