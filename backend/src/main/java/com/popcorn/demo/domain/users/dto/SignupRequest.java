package com.popcorn.demo.domain.users.dto;
import lombok.*;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class SignupRequest {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordCheck;

    private String phone;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotNull(message = "역할은 필수입니다.")
    private UserRole role;
}
