package com.popcorn.demo.domain.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.users.dto.SignupRequest;
import com.popcorn.demo.domain.users.dto.SignupResponse;
import com.popcorn.demo.domain.users.dto.UserResponse;
import com.popcorn.demo.domain.users.dto.UserUpdateRequest;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.service.UserService;

import lombok.RequiredArgsConstructor;

@Tag(name = "User", description = "사용자 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @Operation(
        summary = "회원가입",
        description = """
            새로운 사용자 계정을 생성합니다.

            **주요 기능:**
            - 이메일 중복 검증
            - 비밀번호 암호화 저장
            - 사용자 역할 선택 (CUSTOMER, OWNER, MANAGER)
            - 계정 활성화 상태로 생성

            **입력 검증:**
            - 이메일: 유효한 형식 & 중복 불가
            - 비밀번호: 최소 8자 이상
            - 비밀번호 확인: 비밀번호와 일치해야 함
            - 이름: 필수 입력
            - 전화번호: 11자리 숫자 (선택)
            - 역할: CUSTOMER, OWNER, MANAGER 중 선택

            **역할별 권한:**
            - CUSTOMER: 일반 고객 (주문, 예약)
            - OWNER: 사업자 (팝업 관리, 주문 관리)
            - MANAGER: 관리자 (매장 운영 지원)

            **사용 후 절차:**
            1. 회원가입 완료
            2. /api/v1/auth/login으로 로그인
            3. JWT 토큰으로 API 인증
            """
    )
    @ApiResponse(
        responseCode = "201",
        description = "회원가입 성공",
        content = @Content(
            schema = @Schema(implementation = SignupResponse.class),
            examples = {
                @ExampleObject(
                    name = "고객 회원가입 성공",
                    summary = "CUSTOMER 역할 회원가입 성공",
                    value = """
                        {
                          "userId": 12345,
                          "email": "customer@example.com",
                          "name": "김고객",
                          "role": "CUSTOMER",
                          "message": "회원가입이 완료되었습니다."
                        }
                        """
                ),
                @ExampleObject(
                    name = "사업자 회원가입 성공",
                    summary = "OWNER 역할 회원가입 성공",
                    value = """
                        {
                          "userId": 67890,
                          "email": "popcorn5@popcorn.com",
                          "name": "홍길동",
                          "role": "OWNER",
                          "message": "회원가입이 완료되었습니다."
                        }
                        """
                )
            }
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "입력값 검증 실패",
        content = @Content(
            examples = @ExampleObject(
                name = "검증 실패",
                value = """
                    {
                      "code": 400,
                      "message": "입력값이 올바르지 않습니다.",
                      "details": {
                        "email": "이미 사용 중인 이메일입니다.",
                        "password": "비밀번호는 최소 8자 이상이어야 합니다."
                      }
                    }
                    """
            )
        )
    )
    @PostMapping("/signup")
    public SignupResponse signup(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "회원가입 요청 정보",
            required = true,
            content = @Content(
                schema = @Schema(implementation = SignupRequest.class),
                examples = {
                    @ExampleObject(
                        name = "고객 회원가입",
                        summary = "고객(CUSTOMER) 역할 회원가입",
                        value = """
                            {
                              "email": "customer@example.com",
                              "password": "securePassword123",
                              "passwordCheck": "securePassword123",
                              "name": "김고객",
                              "phone": "01012345678",
                              "role": "CUSTOMER"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "사업자 회원가입",
                        summary = "사업자(OWNER) 역할 회원가입",
                        value = """
                            {
                              "email": "popcorn5@popcorn.com",
                              "password": "testPassword123",
                              "passwordCheck": "testPassword123",
                              "name": "홍길동",
                              "phone": "01012345678",
                              "role": "OWNER"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "관리자 회원가입",
                        summary = "관리자(MANAGER) 역할 회원가입",
                        value = """
                            {
                              "email": "manager@example.com",
                              "password": "managerPassword123",
                              "passwordCheck": "managerPassword123",
                              "name": "이관리",
                              "phone": "01087654321",
                              "role": "MANAGER"
                            }
                            """
                    )
                }
            )
        )
        @Valid @RequestBody SignupRequest request) {

        return userService.register(request);
    }

    @Operation(
        summary = "내 정보 조회",
        description = """
            현재 로그인된 사용자의 개인정보를 조회합니다.

            **인증 요구:**
            - JWT 토큰 필수 (Authorization: Bearer {token})

            **응답 정보:**
            - 사용자 ID, 이메일, 이름
            - 전화번호, 권한 정보
            - 계정 활성화 상태
            - 가입/수정 일시

            **사용 케이스:**
            - 마이페이지 화면 표시
            - 프로필 편집 전 현재 정보 확인
            - 권한 확인용
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "내 정보 조회 성공",
        content = @Content(
            schema = @Schema(implementation = UserResponse.class),
            examples = @ExampleObject(
                name = "내 정보",
                value = """
                    {
                      "userId": 1,
                      "email": "popcorn1@popcorn.com",
                      "name": "PopCorn Test User",
                      "phone": "01012345678",
                      "role": "CUSTOMER",
                      "isActive": true,
                      "createdAt": "2025-01-08T10:00:00",
                      "updatedAt": "2025-01-08T10:00:00"
                    }
                    """
            )
        )
    )
    @ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(
            examples = @ExampleObject(
                name = "인증 실패",
                value = """
                    {
                      "code": 401,
                      "message": "인증이 필요합니다."
                    }
                    """
            )
        )
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/mypage")
    public UserResponse getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        // SecurityContext에서 userId 가져오기
        Long userId = customUserDetails.getUserId();
        System.out.println("SecurityContext에서 가져온 userId: " + userId);

        // DB에서 실제 유저 정보 조회
        User user = userService.getUserById(userId);

        return UserResponse.from(user);
    }

    @Operation(
        summary = "내 정보 수정",
        description = """
            현재 로그인된 사용자의 개인정보를 수정합니다.

            **인증 요구:**
            - JWT 토큰 필수 (Authorization: Bearer {token})

            **수정 가능 항목:**
            - 이름: 2-50자 한글/영문
            - 전화번호: 11자리 숫자 (선택사항)
            - 비밀번호: 최소 8자 이상 (선택사항)

            **수정 불가 항목:**
            - 이메일 (계정 식별자로 고정)
            - 권한 (시스템 관리)
            - 사용자 ID

            **보안:**
            - 비밀번호 변경 시 암호화 저장
            - 수정된 정보는 즉시 반영
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "내 정보 수정 성공",
        content = @Content(
            schema = @Schema(implementation = UserResponse.class),
            examples = @ExampleObject(
                name = "수정 성공",
                value = """
                    {
                      "userId": 1,
                      "email": "popcorn1@popcorn.com",
                      "name": "수정된 이름",
                      "phone": "01087654321",
                      "role": "CUSTOMER",
                      "isActive": true,
                      "createdAt": "2025-01-08T10:00:00",
                      "updatedAt": "2025-01-08T15:30:00"
                    }
                    """
            )
        )
    )
    @ApiResponse(
        responseCode = "400",
        description = "입력값 검증 실패",
        content = @Content(
            examples = @ExampleObject(
                name = "검증 실패",
                value = """
                    {
                      "code": 400,
                      "message": "입력값이 올바르지 않습니다.",
                      "details": {
                        "name": "이름은 2자 이상 50자 이하여야 합니다.",
                        "phone": "전화번호는 11자리 숫자여야 합니다."
                      }
                    }
                    """
            )
        )
    )
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/mypage")
    public UserResponse updateMyInfo(
        @AuthenticationPrincipal CustomUserDetails customUserDetails,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "수정할 사용자 정보",
            required = true,
            content = @Content(
                schema = @Schema(implementation = UserUpdateRequest.class),
                examples = @ExampleObject(
                    name = "정보 수정 요청",
                    value = """
                        {
                          "name": "수정된 이름",
                          "phone": "01087654321",
                          "password": "newSecurePassword123"
                        }
                        """
                )
            )
        )
        @Valid @RequestBody UserUpdateRequest request) {
        // SecurityContext에서 userId 가져오기
        Long userId = customUserDetails.getUserId();
        System.out.println("SecurityContext에서 가져온 userId: " + userId);

        User updatedUser = userService.updateUser(userId, request);
        return UserResponse.from(updatedUser);
    }

    /**
     * 사용자 계정 탈퇴
     */
    /*@DeleteMapping("/me/deactivate")
    public ResponseEntity<Void> deactivateUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deactivateUser(userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }*/

}
