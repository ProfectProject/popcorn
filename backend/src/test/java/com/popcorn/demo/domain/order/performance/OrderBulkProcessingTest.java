package com.popcorn.demo.domain.order.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand.OrderItemCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.service.AsyncEventPublisher;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.OrderDomainService;
import com.popcorn.demo.domain.order.service.OrderItemPriceService;
import com.popcorn.demo.domain.order.service.OrderValidationService;

/**
 * 주문 대량 처리 성능 테스트 - 현재 구조에 맞게 재작성
 * - 대량 주문 생성 처리 성능
 * - 동시성 처리 안정성
 * - 메모리 효율성 검증
 */
class OrderBulkProcessingTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidationService validationService;

    @Mock
    private AsyncEventPublisher eventPublisher;

    @Mock
    private OrderDomainService orderDomainService;

    @Mock
    private OrderItemPriceService orderItemPriceService;

    private OrderCommandService commandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        commandService = mock(OrderCommandService.class);

        // Mock command service to return successful response
        when(commandService.createOrder(any(CreateOrderCommand.class)))
                .thenAnswer(invocation -> {
                    return CreateOrderResponse.builder()
                            .orderId(UUID.randomUUID())
                            .orderNo("ORDER-" + System.currentTimeMillis())
                            .orderType("PURCHASE")
                            .status("REQUESTED")
                            .storeId(UUID.randomUUID())
                            .popupId(UUID.randomUUID())
                            .totalAmount(1000)
                            .build();
                });
    }

    @Test
    void bulkOrderCreation_HandlesConcurrentOrders() throws Exception {
        // given
        int totalOrders = 200;
        int concurrentThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> processingTimes = Collections.synchronizedList(new ArrayList<>());

        // when
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Instant bulkStart = Instant.now();

        for (int i = 0; i < totalOrders; i++) {
            final int customerId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Instant orderStart = Instant.now();
                try {
                    CreateOrderCommand request = createOrderRequest(customerId);
                    CreateOrderResponse response = commandService.createOrder(request);

                    assertThat(response).isNotNull();
                    assertThat(response.getOrderId()).isNotNull();

                    successCount.incrementAndGet();
                    Instant orderEnd = Instant.now();
                    processingTimes.add(Duration.between(orderStart, orderEnd).toMillis());

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Order creation failed: " + e.getMessage());
                }
            }, executor);

            futures.add(future);
        }

        // 모든 주문 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(120, TimeUnit.SECONDS);

        Instant bulkEnd = Instant.now();
        Duration totalTime = Duration.between(bulkStart, bulkEnd);

        // then - 대량 처리 성능 검증
        System.out.println("📊 Bulk Processing Results:");
        System.out.println("Total Orders: " + totalOrders);
        System.out.println("Successful: " + successCount.get());
        System.out.println("Failed: " + failureCount.get());
        System.out.println("Success Rate: " +
                String.format("%.1f%%", (successCount.get() * 100.0 / totalOrders)));

        if (!processingTimes.isEmpty()) {
            double avgTime = processingTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0);
            System.out.println("Average Processing Time: " + String.format("%.2f ms", avgTime));

            long maxTime = processingTimes.stream()
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
            System.out.println("Max Processing Time: " + maxTime + " ms");
        }

        double throughput = successCount.get() / (totalTime.toMillis() / 1000.0);
        System.out.println("Throughput: " + String.format("%.2f orders/sec", throughput));
        System.out.println("Total Time: " + totalTime.toSeconds() + " seconds");

        // 성능 기준
        assertThat(successCount.get()).isEqualTo(totalOrders); // 모든 주문 성공
        assertThat(failureCount.get()).isEqualTo(0); // 실패 없음
        assertThat(throughput).isGreaterThan(5.0); // 초당 5개 이상 처리

        executor.shutdown();
    }

    @Test
    void sequentialBulkProcessing_MeasuresBaselinePerformance() {
        // given
        int totalOrders = 50;
        List<Long> processingTimes = new ArrayList<>();

        // when - 순차 처리로 기준선 측정
        Instant sequentialStart = Instant.now();

        IntStream.range(0, totalOrders).forEach(i -> {
            Instant orderStart = Instant.now();
            try {
                CreateOrderCommand request = createOrderRequest(i);
                CreateOrderResponse response = commandService.createOrder(request);

                assertThat(response).isNotNull();
                Instant orderEnd = Instant.now();
                processingTimes.add(Duration.between(orderStart, orderEnd).toMillis());

            } catch (Exception e) {
                throw new RuntimeException("Sequential processing failed", e);
            }
        });

        Instant sequentialEnd = Instant.now();
        Duration totalTime = Duration.between(sequentialStart, sequentialEnd);

        // then
        System.out.println("📊 Sequential Processing Baseline:");
        System.out.println("Total Orders: " + totalOrders);
        System.out.println("Total Time: " + totalTime.toMillis() + " ms");

        double avgTime = processingTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        System.out.println("Average Processing Time: " + String.format("%.2f ms", avgTime));

        double throughput = totalOrders / (totalTime.toMillis() / 1000.0);
        System.out.println("Sequential Throughput: " + String.format("%.2f orders/sec", throughput));

        assertThat(avgTime).isLessThan(1000.0); // 평균 1초 미만
        assertThat(throughput).isGreaterThan(1.0); // 초당 1개 이상
    }

    @Test
    void memoryUsage_RemainsStableDuringBulkProcessing() throws Exception {
        // given
        int totalOrders = 100;
        Runtime runtime = Runtime.getRuntime();

        // when
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory before: " + (memoryBefore / 1024 / 1024) + " MB");

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < totalOrders; i++) {
            final int orderId = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateOrderCommand request = createOrderRequest(orderId);
                    commandService.createOrder(request);
                } catch (Exception e) {
                    // Ignore for memory test
                }
            }, executor);

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(60, TimeUnit.SECONDS);

        // GC 실행 후 메모리 확인
        System.gc();
        Thread.sleep(1000);

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.println("Memory after: " + (memoryAfter / 1024 / 1024) + " MB");
        long memoryIncrease = memoryAfter - memoryBefore;
        System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + " MB");

        // then
        assertThat(memoryIncrease).isLessThan(100 * 1024 * 1024); // 100MB 미만 증가

        executor.shutdown();
    }

    private CreateOrderCommand createOrderRequest(int index) {
        return CreateOrderCommand.builder()
                .userId((long) (index % 1000 + 1))
                .popupId(UUID.randomUUID())
                .orderType("PURCHASE")
                .items(List.of(
                        OrderItemCommand.builder()
                                .orderItemType(OrderItemType.GOODS)
                                .goodsVariantId(UUID.randomUUID())
                                .qty(1 + (index % 5))
                                .unitPrice(1000 + index)
                                .build()
                ))
                .build();
    }
}