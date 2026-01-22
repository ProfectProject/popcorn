package com.popcorn.users.auth.controller;

import org.springframework.web.bind.annotation.*;

import com.popcorn.users.auth.dto.LoginRequest;
import com.popcorn.users.auth.dto.LoginResponse;
import com.popcorn.users.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("api/users/v1/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(
        summary = "로그인",
        description = """
            이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.

            **주요 기능:**
            - 이메일/비밀번호 기반 인증
            - JWT 액세스 토큰 발급
            - 사용자 권한 정보 반환
            - 로그인 실패 시 상세 오류 메시지

            **JWT 토큰 사용:**
            - Authorization: Bearer {token}
            - 토큰 유효시간: 24시간
            - 권한: CUSTOMER, OWNER, MANAGER

            **테스트 계정:**
            - 이메일: popcorn1@popcorn.com
            - 비밀번호: (실제 해시된 값 필요)
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "로그인 성공",
        content = @Content(
            schema = @Schema(implementation = LoginResponse.class),
            examples = @ExampleObject(
                name = "로그인 성공 응답",
                value = """
                    {
                      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "tokenType": "Bearer",
                      "expiresIn": 86400,
                      "user": {
                        "userId": 1,
                        "email": "popcorn1@popcorn.com",
                        "name": "PopCorn Test User",
                        "role": "CUSTOMER"
                      }
                    }
                    """
            )
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "인증 실패 (잘못된 이메일 또는 비밀번호)",
        content = @Content(
            examples = @ExampleObject(
                name = "로그인 실패",
                value = """
                    {
                      "code": 401,
                      "message": "이메일 또는 비밀번호가 일치하지 않습니다."
                    }
                    """
            )
        )
    )
    @PostMapping("/login")
    public LoginResponse login(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "로그인 요청 정보",
            required = true,
            content = @Content(
                schema = @Schema(implementation = LoginRequest.class),
                examples = @ExampleObject(
                    name = "로그인 요청",
                    value = """
                        {
                          "email": "popcorn1@popcorn.com",
                          "password": "your_password_here"
                        }
                        """
                )
            )
        )
        @Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
