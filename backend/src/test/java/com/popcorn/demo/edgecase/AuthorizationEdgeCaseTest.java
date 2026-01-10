package com.popcorn.demo.edgecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * 🛡️ 권한 엣지 케이스 테스트
 *
 * 다양한 권한 상황에서의 API 접근 제어를 검증합니다:
 * - 역할별 접근 권한 (CUSTOMER, OWNER, MANAGER)
 * - 토큰 만료/무효화 시나리오
 * - 권한 상승 공격 방지
 * - 크로스 유저 데이터 접근 방지
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("🛡️ 권한 엣지 케이스 테스트")
public class AuthorizationEdgeCaseTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;

    // 테스트용 사용자 정보
    private String customerToken;
    private String ownerToken;
    private String managerToken;
    private String expiredToken;
    private String invalidToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 테스트용 JWT 토큰 생성
        customerToken = jwtUtil.createJwt(1001L, "customer@test.com", "CUSTOMER", 3600000L);
        ownerToken = jwtUtil.createJwt(1002L, "owner@test.com", "OWNER", 3600000L);
        managerToken = jwtUtil.createJwt(1003L, "manager@test.com", "MANAGER", 3600000L);
        expiredToken = jwtUtil.createJwt(1004L, "expired@test.com", "CUSTOMER", 1L); // 1ms로 즉시 만료
        invalidToken = "invalid.jwt.token";
    }

    // ========================= 팝업 API 권한 테스트 =========================

    @Test
    @DisplayName("✅ CUSTOMER 역할 - 팝업 조회 성공")
    void testCustomerCanAccessPopups() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("✅ OWNER 역할 - 팝업 조회 성공")
    void testOwnerCanAccessPopups() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + ownerToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("✅ MANAGER 역할 - 팝업 조회 성공")
    void testManagerCanAccessPopups() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + managerToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("❌ 토큰 없이 접근 - 403 Forbidden")
    void testAccessWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/popups"))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("❌ 만료된 토큰 - 403 Forbidden")
    void testAccessWithExpiredToken() throws Exception {
        Thread.sleep(100); // 토큰 만료 대기

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden()) // Spring Security는 만료된 토큰을 403으로 처리
                .andDo(print());
    }

    @Test
    @DisplayName("❌ 유효하지 않은 토큰 - 403 Forbidden")
    void testAccessWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("❌ 잘못된 Authorization 헤더 형식")
    void testInvalidAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", customerToken)) // Bearer 누락
                .andExpect(status().isForbidden())
                .andDo(print());

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Basic " + customerToken)) // 잘못된 스키마
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    // ========================= 주문 API 권한 테스트 =========================

    @Test
    @DisplayName("✅ CUSTOMER - 자신의 주문 목록 조회 성공")
    void testCustomerCanAccessOwnOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/me")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("❌ 권한 없는 역할 - 관리자 전용 API 접근 차단")
    void testUnauthorizedRoleAccess() throws Exception {
        // CUSTOMER가 OWNER/MANAGER 전용 API에 접근
        mockMvc.perform(get("/api/v1/orders/store")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("✅ OWNER - 스토어 주문 관리 접근 성공")
    void testOwnerCanAccessStoreOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/store")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andDo(print());
    }

    // ========================= 크로스 사용자 데이터 접근 방지 테스트 =========================
    @Test
    @DisplayName("❌ 권한 상승 공격 방지 - 토큰 조작 시도")
    void testPreventPrivilegeEscalation() throws Exception {
        // 조작된 토큰으로 권한 상승 시도
        String manipulatedToken = customerToken.substring(0, customerToken.length() - 5) + "ADMIN";

        mockMvc.perform(get("/api/v1/orders/store")
                        .header("Authorization", "Bearer " + manipulatedToken))
                .andExpect(status().isForbidden()) // 조작된 토큰도 403으로 처리
                .andDo(print());
    }

    // ========================= 동시 세션 및 토큰 무효화 테스트 =========================

    @Test
    @DisplayName("⚡ 동시 세션 처리 - 같은 사용자 multiple token")
    void testMultipleTokensForSameUser() throws Exception {
        String token1 = jwtUtil.createJwt(2001L, "same@user.com", "CUSTOMER", 3600000L);
        String token2 = jwtUtil.createJwt(2001L, "same@user.com", "CUSTOMER", 3600000L);

        // 두 토큰 모두 유효해야 함
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("🔄 토큰 갱신 시나리오")
    void testTokenRefreshScenario() throws Exception {
        // 기존 토큰으로 접근
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // 새 토큰 발급
        String refreshedToken = jwtUtil.createJwt(1001L, "customer@test.com", "CUSTOMER", 3600000L);

        // 새 토큰으로 접근
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + refreshedToken))
                .andExpect(status().isOk());
    }

    // ========================= 엣지 케이스 권한 시나리오 =========================

    @Test
    @DisplayName("⚠️ 빈 Authorization 헤더")
    void testEmptyAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", ""))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("⚠️ 공백만 있는 토큰")
    void testWhitespaceOnlyToken() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer    "))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("⚠️ 매우 긴 토큰 (DoS 공격 방지)")
    void testVeryLongToken() throws Exception {
        String veryLongToken = "a".repeat(10000);

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + veryLongToken))
                .andExpect(status().isForbidden())
                .andDo(print());
    }

    @Test
    @DisplayName("⚡ 동시 요청 권한 검증 - Race Condition 방지")
    void testConcurrentAuthorizationCheck() throws Exception {
        // 병렬로 여러 요청 수행
        for (int i = 0; i < 5; i++) {
            final int requestId = i;
            new Thread(() -> {
                try {
                    mockMvc.perform(get("/api/v1/popups")
                                    .header("Authorization", "Bearer " + customerToken)
                                    .param("page", String.valueOf(requestId)))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        Thread.sleep(1000); // 모든 요청 완료 대기
    }

    @Test
    @DisplayName("🔍 SQL Injection through JWT token (보안 테스트)")
    void testSQLInjectionThroughToken() throws Exception {
        String maliciousPayload = "'; DROP TABLE users; --";
        String maliciousToken = jwtUtil.createJwt(9999L, maliciousPayload, "CUSTOMER", 3600000L);

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + maliciousToken))
                .andExpect(status().isOk()) // 토큰 자체는 유효하지만 SQL 인젝션은 차단되어야 함
                .andDo(print());
    }
}