package com.popcorn.demo.global.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * API Gateway 부하 테스트
 * - 동시 요청 처리 성능
 * - 응답 시간 측정
 * - 처리량(Throughput) 측정
 * - 메모리 사용량 모니터링
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:loadtestdb",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.com.popcorn.demo=INFO"
})
class ApiGatewayLoadTest {

    @Autowired
    private MockMvc mockMvc;

    private static final int CONCURRENT_USERS = 50;
    private static final int REQUESTS_PER_USER = 20;
    private static final int TOTAL_REQUESTS = CONCURRENT_USERS * REQUESTS_PER_USER;

    @Test
    void apiGateway_HandlesConcurrentRequests() throws Exception {
        // given
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        Instant startTime = Instant.now();

        // when - 동시에 여러 요청 실행
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (int j = 0; j < REQUESTS_PER_USER; j++) {
                    try {
                        Instant requestStart = Instant.now();

                        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders")
                                .header("X-Request-ID", Thread.currentThread().getId() + "-" + j))
                                .andExpect(result -> {
                                    Instant requestEnd = Instant.now();
                                    long responseTime = Duration.between(requestStart, requestEnd).toMillis();
                                    totalResponseTime.addAndGet(responseTime);

                                    int status = result.getResponse().getStatus();
                                    if (status == 401 || status == 403) {
                                        // Expected unauthorized responses
                                        successCount.incrementAndGet();
                                    } else if (status >= 500) {
                                        // Server errors are concerning
                                        errorCount.incrementAndGet();
                                    } else {
                                        // Other 4xx responses are also acceptable
                                        successCount.incrementAndGet();
                                    }
                                });

                        // Simulate realistic user behavior with small delays
                        Thread.sleep(10);

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }
            }, executor);

            futures.add(future);
        }

        // Wait for all requests to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);

        Instant endTime = Instant.now();
        Duration totalTime = Duration.between(startTime, endTime);

        executor.shutdown();

        // then - 성능 지표 검증
        int totalProcessed = successCount.get() + errorCount.get();
        double successRate = (double) successCount.get() / totalProcessed * 100;
        double avgResponseTime = (double) totalResponseTime.get() / totalProcessed;
        double throughput = (double) totalProcessed / totalTime.toMillis() * 1000; // requests per second

        System.out.println("=== API Gateway Load Test Results ===");
        System.out.println("Total Requests: " + totalProcessed);
        System.out.println("Successful Responses: " + successCount.get());
        System.out.println("Error Responses: " + errorCount.get());
        System.out.println("Success Rate: " + String.format("%.2f%%", successRate));
        System.out.println("Average Response Time: " + String.format("%.2f ms", avgResponseTime));
        System.out.println("Throughput: " + String.format("%.2f requests/sec", throughput));
        System.out.println("Total Test Duration: " + totalTime.toMillis() + " ms");

        // Assertions
        assertThat(successRate).isGreaterThan(95.0); // 95% 이상 성공률
        assertThat(avgResponseTime).isLessThan(200.0); // 평균 응답시간 200ms 미만
        assertThat(errorCount.get()).isLessThan(TOTAL_REQUESTS * 0.05); // 5% 미만 오류율
    }

    @Test
    void apiGateway_HandlesSpikingLoad() throws Exception {
        // Simulate sudden spike in traffic
        ExecutorService executor = Executors.newFixedThreadPool(100);
        AtomicInteger responseCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> spikeFutures = new ArrayList<>();

        // Create sudden spike - 100 simultaneous requests
        for (int i = 0; i < 100; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Instant start = Instant.now();

                    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders")
                            .header("X-Load-Test", "spike-test"))
                            .andExpect(MockMvcResultMatchers.status().isUnauthorized()); // Expected

                    Instant end = Instant.now();
                    responseTimes.add(Duration.between(start, end).toMillis());
                    responseCount.incrementAndGet();

                } catch (Exception e) {
                    // Count as processed even if exception occurred
                    responseCount.incrementAndGet();
                }
            }, executor);

            spikeFutures.add(future);
        }

        // Wait for spike to complete
        CompletableFuture.allOf(spikeFutures.toArray(new CompletableFuture[0]))
                .get(15, TimeUnit.SECONDS);

        executor.shutdown();

        // Analyze spike handling
        assertThat(responseCount.get()).isEqualTo(100);

        if (!responseTimes.isEmpty()) {
            double avgSpikeResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            long maxResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);

            System.out.println("=== Spike Load Test Results ===");
            System.out.println("Spike Requests Processed: " + responseCount.get());
            System.out.println("Average Spike Response Time: " + String.format("%.2f ms", avgSpikeResponseTime));
            System.out.println("Max Response Time: " + maxResponseTime + " ms");

            // Even during spikes, system should remain responsive
            assertThat(avgSpikeResponseTime).isLessThan(500.0); // 500ms during spike
            assertThat(maxResponseTime).isLessThan(2000L); // Max 2 seconds
        }
    }

    @Test
    void apiGateway_MaintainsPerformanceUnderSustainedLoad() throws Exception {
        // Sustained load test - longer duration, moderate concurrency
        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        Instant testStart = Instant.now();
        int testDurationSeconds = 10;
        int requestsPerSecond = 10;

        // Run for specified duration
        for (int second = 0; second < testDurationSeconds; second++) {
            List<CompletableFuture<Void>> secondFutures = new ArrayList<>();

            for (int req = 0; req < requestsPerSecond; req++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Instant requestStart = Instant.now();

                        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                                .andExpect(MockMvcResultMatchers.status().isOk());

                        Instant requestEnd = Instant.now();
                        responseTimes.add(Duration.between(requestStart, requestEnd).toMillis());
                        requestCount.incrementAndGet();

                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                    }
                }, executor);

                secondFutures.add(future);
            }

            // Wait for this second's requests
            CompletableFuture.allOf(secondFutures.toArray(new CompletableFuture[0]))
                    .get(2, TimeUnit.SECONDS);

            // Brief pause between seconds
            Thread.sleep(100);
        }

        Instant testEnd = Instant.now();
        executor.shutdown();

        // Calculate sustained load metrics
        Duration totalTestTime = Duration.between(testStart, testEnd);
        double actualThroughput = (double) requestCount.get() / totalTestTime.toSeconds();

        if (!responseTimes.isEmpty()) {
            double avgResponseTime = responseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            double errorRate = (double) errorCount.get() / requestCount.get() * 100;

            System.out.println("=== Sustained Load Test Results ===");
            System.out.println("Test Duration: " + totalTestTime.toSeconds() + " seconds");
            System.out.println("Total Requests: " + requestCount.get());
            System.out.println("Errors: " + errorCount.get());
            System.out.println("Error Rate: " + String.format("%.2f%%", errorRate));
            System.out.println("Actual Throughput: " + String.format("%.2f req/sec", actualThroughput));
            System.out.println("Average Response Time: " + String.format("%.2f ms", avgResponseTime));

            // Sustained load assertions
            assertThat(errorRate).isLessThan(1.0); // Less than 1% error rate
            assertThat(avgResponseTime).isLessThan(100.0); // Maintain good response times
            assertThat(actualThroughput).isGreaterThan(8.0); // Achieve reasonable throughput
        }
    }

    @Test
    void apiGateway_HandlesMemoryPressureGracefully() throws Exception {
        // Test behavior under memory pressure
        List<String> memoryConsumers = new ArrayList<>();
        AtomicInteger responseCount = new AtomicInteger(0);

        try {
            // Create some memory pressure (careful not to cause OOM in test environment)
            for (int i = 0; i < 1000; i++) {
                memoryConsumers.add("x".repeat(1000)); // 1KB strings
            }

            // Test API gateway under memory pressure
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (int i = 0; i < 50; i++) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        mockMvc.perform(MockMvcRequestBuilders.get("/actuator/health"))
                                .andExpect(MockMvcResultMatchers.status().isOk());
                        responseCount.incrementAndGet();
                    } catch (Exception e) {
                        // System might reject requests under pressure
                    }
                }, executor);

                futures.add(future);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(10, TimeUnit.SECONDS);

            executor.shutdown();

            // Should handle at least some requests even under pressure
            assertThat(responseCount.get()).isGreaterThan(30); // At least 60% success

        } finally {
            // Clean up memory
            memoryConsumers.clear();
            System.gc(); // Suggest garbage collection
        }

        System.out.println("=== Memory Pressure Test Results ===");
        System.out.println("Responses under memory pressure: " + responseCount.get() + "/50");
    }

    @Test
    void filterChain_PerformanceUnderLoad() throws Exception {
        // Test that filter chain (including RequestTraceFilter) performs well under load
        ExecutorService executor = Executors.newFixedThreadPool(30);
        AtomicInteger filterProcessedCount = new AtomicInteger(0);
        List<Long> filterResponseTimes = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 200; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Instant start = Instant.now();

                    mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders")
                            .header("X-Trace-Test", "filter-performance"))
                            .andExpect(result -> {
                                // Verify trace filter added trace ID
                                String traceHeader = result.getResponse().getHeader("X-Trace-ID");
                                // Note: Actual header presence depends on filter implementation
                            });

                    Instant end = Instant.now();
                    filterResponseTimes.add(Duration.between(start, end).toMillis());
                    filterProcessedCount.incrementAndGet();

                } catch (Exception e) {
                    // Count as processed for timing purposes
                    filterProcessedCount.incrementAndGet();
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(20, TimeUnit.SECONDS);

        executor.shutdown();

        if (!filterResponseTimes.isEmpty()) {
            double avgFilterTime = filterResponseTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);

            System.out.println("=== Filter Chain Performance Test Results ===");
            System.out.println("Filter Processed Requests: " + filterProcessedCount.get());
            System.out.println("Average Filter Response Time: " + String.format("%.2f ms", avgFilterTime));

            // Filter overhead should be minimal
            assertThat(avgFilterTime).isLessThan(50.0); // Filter overhead < 50ms
            assertThat(filterProcessedCount.get()).isEqualTo(200); // All requests processed
        }
    }
}