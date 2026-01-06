package com.popcorn.demo.domain.auth.controller;

import org.springframework.web.bind.annotation.*;

import com.popcorn.demo.domain.auth.dto.LoginRequest;
import com.popcorn.demo.domain.auth.dto.LoginResponse;
import com.popcorn.demo.domain.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인 API", description = "email/password로 로그인하고 JWT를 발급합니다.")
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
