package com.popcorn.users.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken; // 테스트용 삭제예정
}
