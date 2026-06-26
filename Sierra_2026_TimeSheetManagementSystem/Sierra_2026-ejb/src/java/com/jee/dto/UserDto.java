package com.jee.dto;

import java.io.Serializable;

/**
 *
 * @author prajnashetty
 */
public class UserDto implements Serializable {
    public Long id;
    public String name;
    
    public UserDto() {}

    public UserDto(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() { return id; }
    public String getName() { return name; }

    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
}