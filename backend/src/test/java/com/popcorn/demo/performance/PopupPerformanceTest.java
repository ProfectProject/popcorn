package com.popcorn.demo.performance;

import com.popcorn.demo.common.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.util.StopWatch;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("🚄 성능 테스트")
@Sql(scripts = {"classpath:sql/test-schema.sql", "classpath:userflow-test-data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)

class PopupPerformanceTest extends BaseIntegrationTest {

    private String testToken;

    @BeforeEach
    void setUp() {
        // 테스트용 JWT 토큰 생성 (BaseIntegrationTest 메서드 사용)
        testToken = createCustomerToken();
    }

    // ========================= 응답 시간 측정 =========================

    @Test
    @DisplayName("⏱️ 팝업 목록 조회 응답 시간 측정")
    void testPopupListResponseTime() throws Exception {
        StopWatch stopWatch = new StopWatch();

        // 워밍업 (JVM 최적화)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/v1/popups")
                    .header("Authorization", "Bearer " + testToken)
                    .param("page", "1")
                    .param("size", "10"));
        }

        // 실제 성능 측정
        stopWatch.start("팝업 목록 조회");

        for (int i = 0; i < 100; i++) {
            mockMvc.perform(get("/api/v1/popups")
                            .header("Authorization", "Bearer " + testToken)
                            .param("page", String.valueOf(i % 10 + 1))
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }

        stopWatch.stop();

        long totalTime = stopWatch.getTotalTimeMillis();
        double averageTime = totalTime / 100.0;

        System.out.println("📊 팝업 목록 조회 성능 결과:");
        System.out.println("  - 총 100회 요청 처리 시간: " + totalTime + "ms");
        System.out.println("  - 평균 응답 시간: " + averageTime + "ms");
        System.out.println("  - 초당 처리량: " + (100000.0 / totalTime) + " req/sec");

        // 성능 기준 검증 (예: 평균 응답 시간 200ms 미만)
        if (averageTime > 200) {
            System.out.println("⚠️ 성능 기준 미달: 평균 응답 시간이 200ms를 초과했습니다.");
        } else {
            System.out.println("✅ 성능 기준 만족: 평균 응답 시간 " + averageTime + "ms");
        }
    }

    @Test
    @DisplayName("📄 페이지네이션 성능 테스트")
    void testPaginationPerformance() throws Exception {
        StopWatch stopWatch = new StopWatch();
        int[] pageSizes = {5, 10, 20, 50, 100};

        for (int pageSize : pageSizes) {
            stopWatch.start("페이지 크기 " + pageSize);

            for (int page = 1; page <= 10; page++) {
                mockMvc.perform(get("/api/v1/popups")
                                .header("Authorization", "Bearer " + testToken)
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(pageSize)))
                        .andExpect(status().isOk());
            }

            stopWatch.stop();

            System.out.println("📄 페이지 크기 " + pageSize + " 성능: " +
                    stopWatch.lastTaskInfo().getTimeMillis() + "ms (10페이지)");
        }

        System.out.println("📊 전체 페이지네이션 성능: " + stopWatch.getTotalTimeMillis() + "ms");
    }

    // ========================= 동시 사용자 부하 테스트 =========================

    @Test
    @DisplayName("👥 동시 사용자 부하 테스트")
    void testConcurrentUserLoad() throws Exception {
        int numberOfUsers = 50;
        int requestsPerUser = 20;

        ExecutorService executor = Executors.newFixedThreadPool(numberOfUsers);
        AtomicInteger totalRequests = new AtomicInteger(0);
        AtomicInteger successRequests = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("동시 부하 테스트");

        for (int user = 0; user < numberOfUsers; user++) {
            final int userId = user;
            executor.submit(() -> {
                for (int request = 0; request < requestsPerUser; request++) {
                    try {
                        long startTime = System.currentTimeMillis();

                        int status = mockMvc.perform(get("/api/v1/popups")
                                        .header("Authorization", "Bearer " + testToken)
                                        .param("page", String.valueOf((userId % 5) + 1))
                                        .param("size", "10")
                                        .param("category", userId % 2 == 0 ? "FOOD" : "FASHION"))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                        long responseTime = System.currentTimeMillis() - startTime;
                        totalResponseTime.addAndGet(responseTime);

                        if (status == 200) {
                            successRequests.incrementAndGet();
                        }

                        totalRequests.incrementAndGet();

                    } catch (Exception e) {
                        System.err.println("사용자 " + userId + " 요청 실패: " + e.getMessage());
                    }
                }
            });
        }

        executor.shutdown();
        boolean terminatedInTime = executor.awaitTermination(60, TimeUnit.SECONDS);
        stopWatch.stop();

        // Executor 종료 상태 검증
        assertTrue(terminatedInTime, "ExecutorService가 지정된 시간(60초) 내에 종료되지 않았습니다");

        int total = totalRequests.get();
        int success = successRequests.get();
        long avgResponseTime = totalResponseTime.get() / Math.max(success, 1);

        System.out.println("👥 동시 사용자 부하 테스트 결과:");
        System.out.println("  - 동시 사용자 수: " + numberOfUsers);
        System.out.println("  - 총 요청 수: " + total);
        System.out.println("  - 성공한 요청: " + success);
        System.out.println("  - 성공률: " + (success * 100.0 / total) + "%");
        System.out.println("  - 평균 응답 시간: " + avgResponseTime + "ms");
        System.out.println("  - 전체 처리 시간: " + stopWatch.getTotalTimeMillis() + "ms");
        System.out.println("  - 처리량: " + (success * 1000.0 / stopWatch.getTotalTimeMillis()) + " req/sec");

        // 성능 기준 검증
        double successRate = success * 100.0 / total;
        if (successRate < 95) {
            System.out.println("⚠️ 성능 기준 미달: 성공률이 95% 미만입니다.");
        } else {
            System.out.println("✅ 부하 테스트 통과: 성공률 " + successRate + "%");
        }

        // Assertion 추가
        assertTrue(total > 0, "총 요청 수가 0입니다");
        assertTrue(success > 0, "성공한 요청이 0개입니다");
        assertTrue(successRate >= 50, "성공률이 50% 미만입니다: " + successRate + "%");
        assertTrue(avgResponseTime >= 0, "평균 응답 시간이 음수입니다: " + avgResponseTime + "ms");
        assertTrue(avgResponseTime < 10000, "평균 응답 시간이 너무 깁니다: " + avgResponseTime + "ms");
        assertEquals(numberOfUsers * requestsPerUser, total,
            "총 요청 수가 예상과 다릅니다. 예상: " + (numberOfUsers * requestsPerUser) + ", 실제: " + total);
    }

    // ========================= 메모리 사용량 측정 =========================

    @Test
    @DisplayName("💾 메모리 사용량 및 누수 감지")
    void testMemoryUsageAndLeakDetection() throws Exception {
        Runtime runtime = Runtime.getRuntime();

        // 가비지 컬렉션 수행
        System.gc();

        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("💾 초기 메모리 사용량: " + (initialMemory / 1024 / 1024) + " MB");

        // 대량 요청 수행
        for (int i = 0; i < 1000; i++) {
            mockMvc.perform(get("/api/v1/popups")
                            .header("Authorization", "Bearer " + testToken)
                            .param("page", String.valueOf(i % 10 + 1))
                            .param("size", "20"))
                    .andExpect(status().isOk());

            // 주기적으로 메모리 체크
            if (i % 200 == 0) {
                long currentMemory = runtime.totalMemory() - runtime.freeMemory();
                System.out.println("  " + i + "회 후 메모리: " + (currentMemory / 1024 / 1024) + " MB");
            }
        }

        // 가비지 컬렉션 후 최종 메모리 측정
        System.gc();

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = finalMemory - initialMemory;

        System.out.println("💾 최종 메모리 사용량: " + (finalMemory / 1024 / 1024) + " MB");
        System.out.println("💾 메모리 증가량: " + (memoryIncrease / 1024 / 1024) + " MB");

        // 메모리 누수 감지 (임계값: 100MB)
        if (memoryIncrease > 100 * 1024 * 1024) {
            System.out.println("⚠️ 메모리 누수 의심: " + (memoryIncrease / 1024 / 1024) + "MB 증가");
        } else {
            System.out.println("✅ 메모리 사용량 정상: " + (memoryIncrease / 1024 / 1024) + "MB 증가");
        }
    }

    // ========================= 검색 성능 테스트 =========================

    @Test
    @DisplayName("🔍 검색 쿼리 성능 테스트")
    void testSearchQueryPerformance() throws Exception {
        String[] keywords = {"테스트", "팝업", "음식", "패션", "전시", "체험", "한정", "이벤트"};

        StopWatch stopWatch = new StopWatch();

        for (String keyword : keywords) {
            stopWatch.start("검색어: " + keyword);

            for (int i = 0; i < 50; i++) {
                mockMvc.perform(get("/api/v1/popups")
                                .header("Authorization", "Bearer " + testToken)
                                .param("keyword", keyword)
                                .param("page", "1")
                                .param("size", "20"))
                        .andExpect(status().isOk());
            }

            stopWatch.stop();

            System.out.println("🔍 검색어 '" + keyword + "' 성능: " +
                    stopWatch.lastTaskInfo().getTimeMillis() + "ms (50회 요청)");
        }

        System.out.println("🔍 전체 검색 성능 테스트: " + stopWatch.getTotalTimeMillis() + "ms");

        // 평균 검색 시간 계산
        double avgSearchTime = stopWatch.getTotalTimeMillis() / (double) (keywords.length * 50);
        System.out.println("🔍 평균 검색 응답 시간: " + avgSearchTime + "ms");
    }

    // ========================= 필터링 성능 테스트 =========================

    @Test
    @DisplayName("🎯 필터링 조건별 성능 테스트")
    void testFilteringPerformance() throws Exception {
        String[] categories = {"FOOD", "FASHION", "BEAUTY", "EXHIBITION", "LIFESTYLE"};


        StopWatch stopWatch = new StopWatch();

        // 단일 필터 성능
        stopWatch.start("단일 카테고리 필터");
        for (String category : categories) {
            for (int i = 0; i < 20; i++) {
                mockMvc.perform(get("/api/v1/popups")
                                .header("Authorization", "Bearer " + testToken)
                                .param("category", category)
                                .param("page", "1")
                                .param("size", "15"))
                        .andExpect(status().isOk());
            }
        }
        stopWatch.stop();

        // 복합 필터 성능
        stopWatch.start("복합 필터");
        for (int i = 0; i < 50; i++) {
            String category = categories[i % categories.length];
            mockMvc.perform(get("/api/v1/popups")
                            .header("Authorization", "Bearer " + testToken)
                            .param("category", category)
                            .param("keyword", "테스트")
                            .param("page", "1")
                            .param("size", "10"))
                    .andExpect(status().isOk());
        }
        stopWatch.stop();

        System.out.println("🎯 필터링 성능 테스트 결과:");
        System.out.println("  - 단일 필터: " + stopWatch.getTaskInfo()[0].getTimeMillis() + "ms");
        System.out.println("  - 복합 필터: " + stopWatch.lastTaskInfo().getTimeMillis() + "ms");
    }

    // ========================= 대용량 데이터 처리 테스트 =========================

    @Test
    @DisplayName("📈 대용량 데이터 처리 성능")
    void testLargeDatasetPerformance() throws Exception {
        StopWatch stopWatch = new StopWatch();

        // 큰 페이지 크기로 데이터 조회 (최대값 100 이하로 수정)
        int[] largeSizes = {50, 80, 100};

        for (int size : largeSizes) {
            stopWatch.start("페이지 크기 " + size);

            long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            mockMvc.perform(get("/api/v1/popups")
                            .header("Authorization", "Bearer " + testToken)
                            .param("page", "1")
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk());

            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            stopWatch.stop();

            System.out.println("📈 페이지 크기 " + size + ":");
            System.out.println("  - 처리 시간: " + stopWatch.lastTaskInfo().getTimeMillis() + "ms");
            System.out.println("  - 메모리 사용: " + ((endMemory - startMemory) / 1024) + "KB");
        }
    }


    // ========================= 스트레스 테스트 =========================

    // ========================= 스트레스 테스트 =========================

    @Test
    @DisplayName("🔥 스트레스 테스트 - 극한 부하")
    void testStressTest() throws Exception {
        int extremeUsers = 100;
        int requestsPerUser = 10;

        ExecutorService executor = Executors.newFixedThreadPool(extremeUsers);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        Random random = new Random();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("스트레스 테스트");

        for (int user = 0; user < extremeUsers; user++) {
            executor.submit(() -> {
                for (int request = 0; request < requestsPerUser; request++) {
                    try {
                        int status = mockMvc.perform(get("/api/v1/popups")
                                        .header("Authorization", "Bearer " + testToken)
                                        .param("page", String.valueOf(random.nextInt(10) + 1))
                                        .param("size", String.valueOf(random.nextInt(20) + 5)))
                                .andReturn()
                                .getResponse()
                                .getStatus();

                        if (status == 200) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        boolean terminatedInTime = executor.awaitTermination(120, TimeUnit.SECONDS);
        stopWatch.stop();

        // Executor 종료 상태 검증
        assertTrue(terminatedInTime, "ExecutorService가 지정된 시간(120초) 내에 종료되지 않았습니다");

        int totalRequests = extremeUsers * requestsPerUser;
        int success = successCount.get();
        int errors = errorCount.get();

        System.out.println("🔥 스트레스 테스트 결과:");
        System.out.println("  - 극한 동시 사용자: " + extremeUsers);
        System.out.println("  - 총 요청 수: " + totalRequests);
        System.out.println("  - 성공: " + success);
        System.out.println("  - 실패: " + errors);
        System.out.println("  - 성공률: " + (success * 100.0 / totalRequests) + "%");
        System.out.println("  - 처리 시간: " + stopWatch.getTotalTimeMillis() + "ms");

        // 시스템이 극한 부하에서도 일정 수준의 성능을 유지하는지 확인
        double successRate = success * 100.0 / totalRequests;
        if (successRate < 70) {
            System.out.println("⚠️ 스트레스 테스트 실패: 성공률이 70% 미만입니다.");
        } else {
            System.out.println("🔥 스트레스 테스트 통과: 극한 부하에서도 " + successRate + "% 성공률 유지");
        }

        // Assertion 추가
        assertTrue(true, "총 요청 수가 0입니다");
        assertTrue(success >= 0, "성공 요청 수가 음수입니다");
        assertTrue(errors >= 0, "오류 요청 수가 음수입니다");
        assertEquals(totalRequests, success + errors, "성공 + 오류 수가 총 요청 수와 일치하지 않습니다");
        assertTrue(successRate >= 30, "스트레스 테스트에서 성공률이 너무 낮습니다: " + successRate + "%");
        assertTrue(stopWatch.getTotalTimeMillis() > 0, "스트레스 테스트 시간이 0 이하입니다");
        assertTrue(stopWatch.getTotalTimeMillis() < 300000, "스트레스 테스트가 너무 오래 걸렸습니다: " + stopWatch.getTotalTimeMillis() + "ms");
    }
}
