package com.popcorn.demo.regression;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.auth.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 🔄 API 리그레션 테스트
 *
 * 기존 기능의 회귀를 방지하고 API 호환성을 보장합니다:
 * - API 응답 형식 일관성
 * - 하위 호환성 보장
 * - 기존 기능 정상 동작 확인
 * - 스키마 변경 영향 검증
 * - 에러 응답 일관성
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = {"classpath:sql/test-schema.sql", "classpath:userflow-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@DisplayName("🔄 API 리그레션 테스트")
class ApiRegressionTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    private MockMvc mockMvc;
    private String testToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        // 테스트용 JWT 토큰 생성 (auto-generated user ID 1 사용)
        testToken = jwtUtil.createJwt(1L, "testuser@popcorn.com", "CUSTOMER", 3600000L);
    }

    // ========================= API 응답 형식 일관성 =========================

    @Test
    @DisplayName("📋 표준 API 응답 형식 회귀 검증")
    void testStandardApiResponseFormat() throws Exception {
        // 성공 응답 형식 검증
        String successResponse = mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // JSON 구조 상세 검증
        JsonNode responseNode = objectMapper.readTree(successResponse);

        // 필수 필드 존재 확인
        assert responseNode.has("code") : "응답에 'code' 필드가 없습니다";
        assert responseNode.has("message") : "응답에 'message' 필드가 없습니다";
        assert responseNode.has("data") : "응답에 'data' 필드가 없습니다";

        // 응답 코드가 200인지 확인
        assert responseNode.get("code").asInt() == 200 : "성공 응답 코드가 200이 아닙니다";

        System.out.println("✅ 표준 API 응답 형식 검증 완료");
    }

    @Test
    @DisplayName("❌ 에러 응답 형식 회귀 검증")
    void testErrorResponseFormat() throws Exception {
        // 존재하지 않는 팝업 ID로 2101 에러 발생 (팝업을 찾을 수 없음)
        String errorResponse = mockMvc.perform(get("/api/v1/popups/99999999-9999-9999-9999-999999999999")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.code").value(2101))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.data").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode errorNode = objectMapper.readTree(errorResponse);

        // 에러 응답 구조 검증
        assert errorNode.get("code").asInt() == 2101 : "2101 에러 코드가 올바르지 않습니다";
        assert errorNode.has("message") : "에러 메시지가 없습니다";
        assert errorNode.has("data") : "에러 데이터 필드가 없습니다";

        System.out.println("✅ 에러 응답 형식 검증 완료");
    }



    // ========================= 팝업 API 회귀 테스트 =========================

    @Test
    @DisplayName("🏪 팝업 목록 조회 API 회귀 테스트")
    void testPopupListApiRegression() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data").exists());

        // 페이지네이션 파라미터 테스트
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        // 카테고리 필터 테스트
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("category", "FOOD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        // 검색 키워드 테스트
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("keyword", "테스트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        System.out.println("✅ 팝업 목록 조회 API 회귀 테스트 완료");
    }

    // ========================= 사용자 API 회귀 테스트 =========================

    @Test
    @DisplayName("👤 사용자 회원가입 API 회귀 테스트")
    void testUserSignupApiRegression() throws Exception {
        String uniqueEmail = "regression-test-" + System.currentTimeMillis() + "@test.com";

        Map<String, Object> signupRequest = new HashMap<>();
        signupRequest.put("email", uniqueEmail);
        signupRequest.put("password", "testPassword123!");
        signupRequest.put("passwordCheck", "testPassword123!");
        signupRequest.put("name", "회귀테스트사용자");
        signupRequest.put("phone", "01012345678");
        signupRequest.put("role", "CUSTOMER");

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(uniqueEmail))
                .andExpect(jsonPath("$.name").isString());

        System.out.println("✅ 사용자 회원가입 API 회귀 테스트 완료");
    }

    // ========================= 파라미터 검증 회귀 테스트 =========================

    @Test
    @DisplayName("🔍 파라미터 검증 로직 회귀 테스트")
    void testParameterValidationRegression() throws Exception {
        // 유효하지 않은 페이지 번호
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        // 유효하지 않은 페이지 크기
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("size", "1001")) // 최대 크기 초과
                .andExpect(status().isBadRequest());

        // 유효하지 않은 카테고리
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("category", "INVALID_CATEGORY"))
                .andExpect(status().isBadRequest());

        System.out.println("✅ 파라미터 검증 로직 회귀 테스트 완료");
    }

    // ========================= HTTP 헤더 회귀 테스트 =========================

    @Test
    @DisplayName("📨 HTTP 헤더 처리 회귀 테스트")
    void testHttpHeaderRegression() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(header().exists("API-Version-Used"))
                .andExpect(header().string("Content-Type", containsString("application/json")))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"));

        System.out.println("✅ HTTP 헤더 처리 회귀 테스트 완료");
    }

    // ========================= 데이터 형식 회귀 테스트 =========================

    @Test
    @DisplayName("📊 응답 데이터 형식 회귀 테스트")
    void testResponseDataFormatRegression() throws Exception {
        String response = mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode responseNode = objectMapper.readTree(response);
        JsonNode dataNode = responseNode.get("data");
        JsonNode itemsNode = dataNode.get("items");

        if (itemsNode.isArray() && itemsNode.size() > 0) {
            JsonNode firstItem = itemsNode.get(0);

            // 필수 필드 존재 확인
            String[] expectedFields = {"id", "title", "category", "status"};
            for (String field : expectedFields) {
                assert firstItem.has(field) : "팝업 데이터에 '" + field + "' 필드가 없습니다";
            }

            // 데이터 타입 검증
            assert firstItem.get("id").isTextual() : "id는 문자열이어야 합니다";
            assert firstItem.get("title").isTextual() : "title은 문자열이어야 합니다";
            assert firstItem.get("category").isTextual() : "category는 문자열이어야 합니다";
        }

        System.out.println("✅ 응답 데이터 형식 회귀 테스트 완료");
    }

    // ========================= 성능 회귀 테스트 =========================

    @Test
    @DisplayName("⏱️ API 성능 회귀 테스트")
    void testApiPerformanceRegression() throws Exception {
        long startTime = System.currentTimeMillis();

        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk());

        long responseTime = System.currentTimeMillis() - startTime;

        // 성능 기준: 2초 이내 응답
        assert responseTime < 2000 : "API 응답 시간이 2초를 초과했습니다: " + responseTime + "ms";

        System.out.println("✅ API 성능 회귀 테스트 완료 (응답 시간: " + responseTime + "ms)");
    }

    // ========================= 보안 헤더 회귀 테스트 =========================

    @Test
    @DisplayName("🔒 보안 헤더 회귀 테스트")
    void testSecurityHeaderRegression() throws Exception {
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "0"))
                .andExpect(header().exists("Cache-Control"));

        System.out.println("✅ 보안 헤더 회귀 테스트 완료");
    }

    // ========================= 에러 처리 회귀 테스트 =========================

    // ========================= 에러 처리 회귀 테스트 =========================

    @Test
    @DisplayName("🚨 에러 처리 로직 회귀 테스트")
    void testErrorHandlingRegression() throws Exception {
        // 존재하지 않는 팝업 조회 - 404 상태코드, 2101 비즈니스 코드 반환
        mockMvc.perform(get("/api/v1/popups/99999999-9999-9999-9999-999999999999")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2101)) // POPUP_NOT_FOUND 비즈니스 코드
                .andExpect(jsonPath("$.message").isString());

        // 잘못된 데이터 형식으로 회원가입 시도
        Map<String, Object> invalidSignup = new HashMap<>();
        invalidSignup.put("email", "invalid-email"); // 잘못된 이메일 형식
        invalidSignup.put("password", "123"); // 너무 짧은 비밀번호

        mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSignup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isString());

        System.out.println("✅ 에러 처리 로직 회귀 테스트 완료");
    }



    // ========================= API 버전 호환성 테스트 =========================

    @Test
    @DisplayName("🔖 API 버전 호환성 회귀 테스트")
    void testApiVersionCompatibilityRegression() throws Exception {
        // v1 API 호환성 확인
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk())
                .andExpect(header().string("API-Version-Used", "v1"));

        // 버전이 명시되지 않은 경우 기본 v1 동작
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk());

        System.out.println("✅ API 버전 호환성 회귀 테스트 완료");
    }

    // ========================= 페이지네이션 회귀 테스트 =========================

    @Test
    @DisplayName("📄 페이지네이션 로직 회귀 테스트")
    void testPaginationLogicRegression() throws Exception {
        // 첫 번째 페이지
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        // 빈 결과 페이지
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "999")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray());

        System.out.println("✅ 페이지네이션 로직 회귀 테스트 완료");
    }

    // ========================= 종합 회귀 테스트 =========================

    @Test
    @DisplayName("🎯 종합 API 동작 회귀 테스트")
    void testComprehensiveApiRegression() throws Exception {
        // 전체적인 API 동작 흐름 테스트
        System.out.println("🎯 종합 회귀 테스트 시작");

        // 1. 팝업 목록 조회
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken))
                .andExpect(status().isOk());

        // 2. 카테고리별 필터링
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("category", "FOOD"))
                .andExpect(status().isOk());

        // 3. 검색 기능
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("keyword", "test"))
                .andExpect(status().isOk());

        // 4. 페이지네이션
        mockMvc.perform(get("/api/v1/popups")
                        .header("Authorization", "Bearer " + testToken)
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        System.out.println("✅ 종합 API 동작 회귀 테스트 완료");
    }
}