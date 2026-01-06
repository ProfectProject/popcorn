package com.popcorn.demo.domain.users.dto;
import lombok.*;
import com.popcorn.demo.domain.users.entity.enums.UserRole;

@Getter
@Setter
public class SignupRequest {
    private String email;
    private String password;
    private String passwordCheck;
    private String phone;
    private String name;
    private UserRole role;
}
