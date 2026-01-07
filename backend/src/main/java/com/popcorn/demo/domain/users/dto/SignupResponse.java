package com.popcorn.demo.domain.users.dto;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import lombok.*;

@Getter
@Setter
@Builder
public class SignupResponse {
    private String name;
    private String email;
    private UserRole role;
}
