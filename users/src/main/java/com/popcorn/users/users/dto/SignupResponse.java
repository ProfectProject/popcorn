package com.popcorn.users.users.dto;
import com.popcorn.users.users.entity.enums.UserRole;
import lombok.*;

@Getter
@Setter
@Builder
public class SignupResponse {
    private String name;
    private String email;
    private UserRole role;
}
