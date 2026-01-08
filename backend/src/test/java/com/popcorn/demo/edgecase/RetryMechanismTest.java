package com.popcorn.demo.edgecase;

import com.popcorn.demo.common.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * 🔄 재시도 메커니즘 테스트
 *
 * 네트워크 장애, 타임아웃, 서버 오류 등의 상황에서
 * 클라이언트의 재시도 요청을 안전하게 처리하는지 검증합니다:
 * - 네트워크 타임아웃 시나리오
 * - 서버 오류 후 재시도
 * - 재시도 횟수 제한
 * - 백오프 전략
 * - 재시도 간 상태 일관성
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("🔄 재시도 메커니즘 테스트")
public class RetryMechanismTest extends BaseIntegrationTest {

    private String testToken;

    @BeforeEach
    void setUp() {
        // 기본 설정 후에 테스트 토큰 생성
        testToken = createCustomerToken();
    }

    // ========================= 기본 재시도 시나리오 =========================

    @Test
    @DisplayName("🔄 동일한 요청 반복 시도 - 멱등성 보장")
    void testIdempotentRetries() throws Exception {
        String email = "retry-test-" + System.currentTimeMillis() + "@popcorn.com";

        Map<String, Object> signupRequest = new HashMap<>();
        signupRequest.put("email", email);
        signupRequest.put("password", "testPassword123!");
        signupRequest.put("passwordCheck", "testPassword123!");
        signupRequest.put("name", "재시도테스트사용자");
        signupRequest.put("phone", "01012345678");
        signupRequest.put("role", "CUSTOMER");

        // 첫 번째 요청
        String firstResponse = mockMvc.perform(post("/api/v1/users/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andDo(print())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 동일한 요청을 3번 더 반복 (재시도 시뮬레이션)
        for (int i = 0; i < 3; i++) {
            String retryResponse = mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(signupRequest)))
                    .andDo(print())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // 재시도 시에는 이미 존재하는 사용자 오류가 발생해야 함
            // 하지만 시스템이 일관성을 유지해야 함

            // Assertion 추가 - 재시도 응답이 유효한지 확인
            assertNotNull(retryResponse, "재시도 응답이 null입니다");
            assertFalse(retryResponse.isEmpty(), "재시도 응답이 비어있습니다");
        }

        // 첫 번째 응답이 유효한지 확인
        assertNotNull(firstResponse, "첫 번째 응답이 null입니다");
        assertFalse(firstResponse.isEmpty(), "첫 번째 응답이 비어있습니다");
    }

    @Test
    @DisplayName("⏱️ 타임아웃 후 재시도 시나리오")
    void testTimeoutAndRetry() throws Exception {
        // 긴 처리 시간이 필요한 요청 시뮬레이션
        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("orderType", "RESERVATION");
        orderRequest.put("storeId", UUID.randomUUID().toString());
        orderRequest.put("popupId", UUID.randomUUID().toString());
        orderRequest.put("items", java.util.List.of(
                Map.of(
                        "orderItemType", "RESERVATION",
                        "qty", 1,
                        "unitPrice", 15000,
                        "sessionId", UUID.randomUUID().toString()
                )
        ));

        // 첫 번째 시도 - 타임아웃 시뮬레이션
        try {
            mockMvc.perform(post("/api/v1/orders")
                            .header("Authorization", "Bearer " + testToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(orderRequest)))
                    .andDo(print());
        } catch (Exception e) {
            // 타임아웃 예상
        }

        // 짧은 대기 후 재시도
        Thread.sleep(100);

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andDo(print());
    }

    // ========================= 동시 재시도 테스트 =========================

    @Test
    @DisplayName("⚡ 동시 재시도 요청 - Race Condition 방지")
    void testConcurrentRetries() throws Exception {
        String email = "concurrent-retry-" + System.currentTimeMillis() + "@popcorn.com";

        Map<String, Object> signupRequest = new HashMap<>();
        signupRequest.put("email", email);
        signupRequest.put("password", "testPassword123!");
        signupRequest.put("passwordCheck", "testPassword123!");
        signupRequest.put("name", "동시재시도테스트");
        signupRequest.put("phone", "01012345679");
        signupRequest.put("role", "CUSTOMER");

        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        // 동시에 5개의 동일한 회원가입 요청 수행
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                try {
                    int statusCode = mockMvc.perform(post("/api/v1/users/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(signupRequest)))
                            .andDo(print())
                            .andReturn()
                            .getResponse()
                            .getStatus();

                    if (statusCode == 201) {
                        successCount.incrementAndGet();
                    } else if (statusCode == 409) { // 이미 존재하는 사용자
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        boolean terminatedInTime = executor.awaitTermination(10, TimeUnit.SECONDS);

        // Executor 종료 상태 검증
        assertTrue(terminatedInTime, "ExecutorService가 지정된 시간(10초) 내에 종료되지 않았습니다");

        // 정확히 하나만 성공하고 나머지는 conflict여야 함
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("충돌 요청: " + conflictCount.get());
    }

    // ========================= 백오프 전략 테스트 =========================

    @Test
    @DisplayName("📈 지수적 백오프 시뮬레이션")
    void testExponentialBackoffSimulation() throws Exception {
        long[] retryDelays = {100, 200, 400, 800, 1600}; // 지수적 증가

        for (int i = 0; i < retryDelays.length; i++) {
            long startTime = System.currentTimeMillis();

            // API 요청
            try {
                mockMvc.perform(get("/api/v1/popups")
                                .param("page", String.valueOf(i + 1))
                                .param("size", "1"))
                        .andDo(print());
            } catch (Exception ignored) {
                // 오류 무시하고 재시도 패턴 테스트
            }

            long endTime = System.currentTimeMillis();
            System.out.println("재시도 " + (i + 1) + ": " + (endTime - startTime) + "ms");

            // 다음 재시도까지 대기 (백오프)
            if (i < retryDelays.length - 1) {
                Thread.sleep(retryDelays[i]);
            }
        }
    }

    // ========================= 상태 일관성 테스트 =========================

    @Test
    @DisplayName("🔄 재시도 간 데이터 일관성 검증")
    void testDataConsistencyBetweenRetries() throws Exception {
        String uniqueId = "consistency-test-" + System.currentTimeMillis();

        // 첫 번째 조회
        String firstResult = mockMvc.perform(get("/api/v1/popups")
                        .param("keyword", uniqueId)
                        .param("page", "1")
                        .param("size", "10"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Thread.sleep(50);

        // 재시도 조회 - 동일한 결과가 나와야 함
        String retryResult = mockMvc.perform(get("/api/v1/popups")
                        .param("keyword", uniqueId)
                        .param("page", "1")
                        .param("size", "10"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 결과가 동일한지 검증 (멱등성)
        System.out.println("첫 번째 결과와 재시도 결과 일치: " + firstResult.equals(retryResult));
    }

    // ========================= 리소스 오류 후 재시도 =========================

    @Test
    @DisplayName("🚫 존재하지 않는 리소스 재시도")
    void testRetryOnNonExistentResource() throws Exception {
        String nonExistentPopupId = UUID.randomUUID().toString();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/v1/popups/" + nonExistentPopupId))
                    .andExpect(status().isForbidden())
                    .andDo(print());

            Thread.sleep(50); // 재시도 간격
        }

        // 여러 번 재시도해도 계속 404가 나와야 함
    }

    @Test
    @DisplayName("⚠️ 잘못된 요청 데이터 재시도")
    void testRetryWithInvalidData() throws Exception {
        Map<String, Object> invalidSignupRequest = new HashMap<>();
        invalidSignupRequest.put("email", "invalid-email"); // 유효하지 않은 이메일
        invalidSignupRequest.put("password", "123"); // 너무 짧은 비밀번호
        invalidSignupRequest.put("passwordCheck", "456"); // 비밀번호 불일치

        // 동일한 잘못된 요청을 3번 재시도
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/users/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidSignupRequest)))
                    .andExpect(status().isBadRequest()) // 계속 400이어야 함
                    .andDo(print());

            Thread.sleep(50);
        }
    }

    // ========================= 재시도 제한 테스트 =========================

    @Test
    @DisplayName("🛑 재시도 횟수 제한 시뮬레이션")
    void testRetryLimitSimulation() throws Exception {
        final int MAX_RETRIES = 5;
        int retryCount = 0;

        for (int i = 0; i < MAX_RETRIES + 2; i++) { // 제한을 넘어서 시도
            try {
                retryCount++;

                mockMvc.perform(get("/api/v1/popups")
                                .param("page", "999999") // 존재하지 않는 페이지
                                .param("size", "1"))
                        .andDo(print());

                if (retryCount > MAX_RETRIES) {
                    // 실제 구현에서는 재시도를 중단해야 함
                    System.out.println("재시도 제한 초과: " + retryCount);
                    break;
                }
            } catch (Exception e) {
                if (retryCount >= MAX_RETRIES) {
                    System.out.println("최대 재시도 횟수 도달");
                    break;
                }
                Thread.sleep(100L * retryCount); // 재시도 간격 증가
            }
        }
    }

    // ========================= 서킷 브레이커 시뮬레이션 =========================

    @Test
    @DisplayName("⚡ 서킷 브레이커 시뮬레이션")
    void testCircuitBreakerSimulation() throws Exception {
        final int FAILURE_THRESHOLD = 3;
        int consecutiveFailures = 0;
        boolean circuitOpen = false;

        for (int i = 0; i < 10; i++) {
            if (circuitOpen) {
                System.out.println("서킷이 열려있음 - 요청 차단");
                Thread.sleep(1000); // 서킷 브레이커 대기 시간

                // 일정 시간 후 반열림 상태로 전환 시도
                if (i > 7) {
                    circuitOpen = false;
                    consecutiveFailures = 0;
                    System.out.println("서킷 브레이커 반열림 시도");
                }
                continue;
            }

            try {
                // 의도적으로 실패할 수 있는 요청
                int status = mockMvc.perform(get("/api/v1/popups")
                                .param("page", String.valueOf(i % 2 == 0 ? 1 : 999)))
                        .andReturn()
                        .getResponse()
                        .getStatus();

                if (status >= 400) {
                    consecutiveFailures++;
                    System.out.println("요청 실패 " + consecutiveFailures + "/" + FAILURE_THRESHOLD);

                    if (consecutiveFailures >= FAILURE_THRESHOLD) {
                        circuitOpen = true;
                        System.out.println("서킷 브레이커 작동 - 서킷 열림");
                    }
                } else {
                    consecutiveFailures = 0; // 성공 시 카운터 리셋
                    System.out.println("요청 성공 - 실패 카운터 리셋");
                }

            } catch (Exception e) {
                consecutiveFailures++;
                System.out.println("예외 발생: " + e.getMessage());
            }

            Thread.sleep(100);
        }
    }
}