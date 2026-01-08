package com.popcorn.demo.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * 모든 통합 테스트가 상속받을 기본 테스트 클래스
 *
 * 이 클래스는 다음을 제공합니다:
 * - 올바른 테스트 프로파일 설정 (@ActiveProfiles("test"))
 * - MockMvc 설정
 * - JWT 토큰 생성 유틸리티
 * - 공통 테스트 의존성들
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected WebApplicationContext webApplicationContext;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtil jwtUtil;

    protected MockMvc mockMvc;

    @BeforeEach
    void setUpBase() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    /**
     * 테스트용 JWT 토큰 생성 헬퍼 메서드
     */
    protected String createTestToken(Long userId, String email, String role) {
        return jwtUtil.createJwt(userId, email, role, 3600000L);
    }

    /**
     * 기본 고객 테스트 토큰 생성
     */
    protected String createCustomerToken() {
        return createTestToken(1001L, "test@test.com", "CUSTOMER");
    }

    /**
     * 기본 사업자 테스트 토큰 생성
     */
    protected String createOwnerToken() {
        return createTestToken(1002L, "owner@test.com", "OWNER");
    }

    /**
     * 기본 관리자 테스트 토큰 생성
     */
    protected String createManagerToken() {
        return createTestToken(1003L, "manager@test.com", "MANAGER");
    }
}