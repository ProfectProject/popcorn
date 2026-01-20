package com.popcorn.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.DemoApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 통합 테스트용 기본 클래스
 *
 * 이 클래스는 Spring Boot 통합 테스트를 위한 공통 설정과 유틸리티 메서드를 제공합니다.
 */
@SpringBootTest(
    classes = DemoApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.yml")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * 테스트용 고객 토큰 생성
     * @return JWT 토큰
     */
    protected String createCustomerToken() {
        // 간단한 테스트용 토큰 생성
        // 실제 구현에서는 JWT 토큰을 생성하거나 모킹된 토큰을 사용
        return "test-customer-token-" + System.currentTimeMillis();
    }

    /**
     * 테스트용 관리자 토큰 생성
     * @return JWT 토큰
     */
    protected String createAdminToken() {
        return "test-admin-token-" + System.currentTimeMillis();
    }

    /**
     * 테스트용 사용자 회원가입 요청 데이터 생성
     */
    protected Map<String, Object> createSignupRequest(String email, String name) {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "testPassword123!");
        request.put("passwordCheck", "testPassword123!");
        request.put("name", name);
        request.put("phone", "01012345678");
        request.put("role", "CUSTOMER");
        return request;
    }

    /**
     * 테스트용 주문 요청 데이터 생성
     */
    protected Map<String, Object> createOrderRequest(String popupId) {
        Map<String, Object> request = new HashMap<>();
        request.put("orderType", "RESERVATION");
        request.put("popupId", popupId);
        request.put("items", java.util.List.of(
                Map.of(
                        "orderItemType", "RESERVATION",
                        "qty", 1,
                        "unitPrice", 15000,
                        "sessionId", java.util.UUID.randomUUID().toString()
                )
        ));
        return request;
    }
}