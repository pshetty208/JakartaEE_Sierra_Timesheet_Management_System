package com.jee.services;

import com.jee.dto.UserDto;
import jakarta.ejb.Remote;
import java.util.List;

/**
 *
 * @author prajnashetty
 */
@Remote
public interface UserService {
    
    public void save(String name);

    public List<UserDto> getAll();
}
