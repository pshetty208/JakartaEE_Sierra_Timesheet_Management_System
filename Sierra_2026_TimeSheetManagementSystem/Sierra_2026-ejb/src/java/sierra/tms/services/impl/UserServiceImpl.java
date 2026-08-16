package sierra.tms.services.impl;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;
import sierra.tms.dao.UserDao;
import sierra.tms.dto.UserDto;
import sierra.tms.entities.UserEntity;
import sierra.tms.services.UserService;

@Stateless
@RolesAllowed("ADMIN")
public class UserServiceImpl implements UserService{
    
    @EJB
    private UserDao dao;
    
    @Override
    public void save(String name) {

        UserEntity user = new UserEntity();
        user.setName(name);

        dao.save(user);
    }

    @Override
    public List<UserDto> getAll() {
        return dao.findAll()
                .stream()
                .map(this::createDTO)
                .toList();
    }
    
    private UserDto createDTO(UserEntity entity) {
        return new UserDto(
                entity.getId(),
                entity.getName()
        );
    }

}
