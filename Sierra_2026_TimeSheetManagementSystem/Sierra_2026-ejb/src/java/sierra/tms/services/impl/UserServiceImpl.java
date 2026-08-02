package com.jee.services.impl;

import com.jee.dao.UserDao;
import com.jee.dto.UserDto;
import com.jee.entities.UserEntity;
import com.jee.services.UserService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import java.util.List;

/**
 *
 * @author prajnashetty
 */
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
