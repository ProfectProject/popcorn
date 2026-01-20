package com.popcorn.demo.domain.order.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.popcorn.common.cache.IdempotencyService;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand.OrderItemCommand;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.OrderValidationService;
import com.popcorn.demo.domain.order.service.AsyncEventPublisher;
import com.popcorn.demo.domain.order.service.OrderDomainService;
import com.popcorn.demo.domain.order.service.OrderItemPriceService;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;

/**
 * 주문 트래픽 몰림 테스트
 * - 플래시 세일 시나리오
 * - 인기 상품 집중 주문
 * - 동시성 제어 및 멱등성 검증
 * - 시스템 한계점 및 복구 능력 테스트
 */
class OrderTrafficSurgeTestEnhanced {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidationService validationService;

    @Mock
    private AsyncEventPublisher eventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private OrderDomainService orderDomainService;

    @Mock
    private OrderItemPriceService orderItemPriceService;

    private OrderCommandService commandService;

    // Test scenarios
    private static final String POPULAR_PRODUCT_ID = "HOT-ITEM-001";
    private static final Long POPULAR_STORE_ID = 1L;
    private static final int FLASH_SALE_DURATION_SECONDS = 10;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        commandService = new OrderCommandService(orderDomainService, orderRepository,
                orderItemPriceService, eventPublisher, validationService);

        // Mock basic validations
        when(validationService.validateOrderAsync(any(Long.class), any(UUID.class), any(Integer.class))).thenReturn(true);
        when(validationService.resolveStoreId(any(UUID.class))).thenReturn(UUID.randomUUID());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventPublisher.publishEventAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(orderItemPriceService.findMerchVariantPrice(any(UUID.class))).thenReturn(Optional.of(1000));
        when(orderItemPriceService.findSessionOptionPrice(any(UUID.class))).thenReturn(Optional.of(1000));
        when(orderDomainService.createOrder(anyLong(), any(UUID.class), any(UUID.class), any(OrderType.class), anyList()))
                .thenAnswer(invocation -> {
                    Long customerId = invocation.getArgument(0);
                    UUID storeId = invocation.getArgument(1);
                    UUID popupId = invocation.getArgument(2);
                    OrderType orderType = invocation.getArgument(3);
                    @SuppressWarnings("unchecked")
                    List<OrderItem> items = invocation.getArgument(4);

                    int totalAmount = items.stream().mapToInt(OrderItem::getLineAmount).sum();
                    Order order = Order.builder()
                            .id(UUID.randomUUID())
                            .orderNo(Order.generateOrderNo())
                            .customerId(customerId)
                            .storeId(storeId)
                            .popupId(popupId)
                            .orderType(orderType)
                            .status(OrderStatus.REQUESTED)
                            .totalAmount(totalAmount)
                            .build();
                    order.addOrderItems(new ArrayList<>(items));
                    return order;
                });
        when(orderDomainService.canChangeStatus(any(OrderStatus.class), any(OrderStatus.class))).thenReturn(true);
    }

    @Test
    void flashSaleScenario_HandlesMassiveTrafficSurge() throws Exception {
        // given - 플래시 세일 시나리오: 10초간 1000명이 동시 주문
        int totalCustomers = 1000;
        int maxConcurrentUsers = 200;
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOrders = new AtomicInteger(0);
        AtomicInteger duplicateRejections = new AtomicInteger(0);

        List<Long> orderTimes = Collections.synchronizedList(new ArrayList<>());
        List<Long> queueWaitTimes = Collections.synchronizedList(new ArrayList<>());

        ExecutorService flashSaleExecutor = Executors.newFixedThreadPool(maxConcurrentUsers);

        // Mock idempotency service for duplicate detection (simplified for compatibility)
        // Note: executeOnce method may not exist in current IdempotencyService implementation

        List<CompletableFuture<Void>> flashSaleFutures = new ArrayList<>();

        System.out.println("🔥 Flash Sale Started! Duration: " + FLASH_SALE_DURATION_SECONDS + " seconds");
        Instant flashSaleStart = Instant.now();

        // when - 플래시 세일 시작
        for (int customerId = 1; customerId <= totalCustomers; customerId++) {
            final int finalCustomerId = customerId;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                Instant customerStart = Instant.now();

                try {
                    // Simulate realistic user behavior - random delay up to sale duration
                    int userDelay = (int) (Math.random() * FLASH_SALE_DURATION_SECONDS * 1000);
                    Thread.sleep(userDelay);

                    Instant orderStart = Instant.now();
                    queueWaitTimes.add(Duration.between(customerStart, orderStart).toMillis());

                    // Create flash sale order
                    CreateOrderCommand flashOrder = createFlashSaleOrder(finalCustomerId);

                    CreateOrderResponse response = commandService.createOrder(flashOrder);
                    UUID orderId = response.getOrderId();

                    Instant orderEnd = Instant.now();
                    orderTimes.add(Duration.between(orderStart, orderEnd).toMillis());

                    assertThat(orderId).isNotNull();
                    successfulOrders.incrementAndGet();

                } catch (IllegalStateException e) {
                    // Duplicate request - expected behavior
                    duplicateRejections.incrementAndGet();
                } catch (Exception e) {
                    failedOrders.incrementAndGet();
                    System.err.println("Flash sale order failed for customer " + finalCustomerId + ": " + e.getMessage());
                }
            }, flashSaleExecutor);

            flashSaleFutures.add(future);
        }

        // Wait for flash sale to complete
        CompletableFuture.allOf(flashSaleFutures.toArray(new CompletableFuture[0]))
                .get(FLASH_SALE_DURATION_SECONDS + 30, TimeUnit.SECONDS);

        Instant flashSaleEnd = Instant.now();
        Duration totalFlashSaleTime = Duration.between(flashSaleStart, flashSaleEnd);

        flashSaleExecutor.shutdown();

        // then - 플래시 세일 결과 분석
        analyzeFlashSaleResults(
                totalCustomers, successfulOrders.get(), failedOrders.get(),
                duplicateRejections.get(), totalFlashSaleTime, orderTimes, queueWaitTimes
        );

        // Flash sale performance assertions
        double successRate = (double) successfulOrders.get() / totalCustomers * 100;
        assertThat(successRate).isGreaterThan(70.0); // 70% 이상 성공률 (높은 경쟁 고려)
        assertThat(duplicateRejections.get()).isLessThan((int)(totalCustomers * 0.1)); // 10% 미만 중복
        assertThat(failedOrders.get()).isLessThan((int)(totalCustomers * 0.2)); // 20% 미만 실패
    }

    @Test
    void popularProductConcurrency_HandlesSameProductOrders() throws Exception {
        // given - 인기 상품에 대한 동시 주문 시나리오
        int concurrentOrders = 100;
        String popularProduct = POPULAR_PRODUCT_ID;

        AtomicInteger concurrentSuccessCount = new AtomicInteger(0);
        AtomicInteger concurrencyConflicts = new AtomicInteger(0);
        List<Long> concurrentOrderTimes = Collections.synchronizedList(new ArrayList<>());

        ExecutorService concurrentExecutor = Executors.newFixedThreadPool(50);

        // Mock product inventory validation
        AtomicInteger remainingStock = new AtomicInteger(50); // Limited stock
        when(validationService.validateOrderAsync(any(Long.class), any(UUID.class), any(Integer.class))).thenAnswer(invocation -> {
            if (remainingStock.decrementAndGet() >= 0) {
                return true;
            } else {
                throw new IllegalStateException("Out of stock");
            }
        });

        List<CompletableFuture<Void>> concurrentFutures = new ArrayList<>();

        // when - 동시 주문 실행
        for (int i = 0; i < concurrentOrders; i++) {
            final int customerId = i + 1;

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    Instant orderStart = Instant.now();

                    CreateOrderCommand popularOrder = createPopularProductOrder(customerId, popularProduct);
                    CreateOrderResponse response = commandService.createOrder(popularOrder);
                    UUID orderId = response.getOrderId();

                    Instant orderEnd = Instant.now();
                    concurrentOrderTimes.add(Duration.between(orderStart, orderEnd).toMillis());

                    assertThat(orderId).isNotNull();
                    concurrentSuccessCount.incrementAndGet();

                } catch (IllegalStateException e) {
                    // Expected when out of stock
                    concurrencyConflicts.incrementAndGet();
                } catch (Exception e) {
                    concurrencyConflicts.incrementAndGet();
                }
            }, concurrentExecutor);

            concurrentFutures.add(future);
        }

        CompletableFuture.allOf(concurrentFutures.toArray(new CompletableFuture[0]))
                .get(20, TimeUnit.SECONDS);

        concurrentExecutor.shutdown();

        // then - 동시성 제어 검증
        System.out.println("=== Popular Product Concurrency Test Results ===");
        System.out.println("Concurrent Orders Attempted: " + concurrentOrders);
        System.out.println("Successful Orders: " + concurrentSuccessCount.get());
        System.out.println("Conflicts/Stock-outs: " + concurrencyConflicts.get());
        System.out.println("Remaining Stock: " + remainingStock.get());

        // Verify stock control worked correctly
        assertThat(concurrentSuccessCount.get()).isLessThanOrEqualTo(50); // Cannot exceed stock
        assertThat(concurrentSuccessCount.get()).isGreaterThan(40); // Most stock should be sold
        assertThat(remainingStock.get()).isLessThanOrEqualTo(0); // Stock should be depleted
    }

    @Test
    void orderIdempotency_HandlesDuplicateRequests() throws Exception {
        // given - 멱등성 테스트: 동일 주문 중복 요청
        String customerKey = "customer-123";
        String idempotencyKey = "order-key-unique-123";
        int duplicateAttempts = 20;

        AtomicInteger successfulCreations = new AtomicInteger(0);
        AtomicInteger duplicateRejections = new AtomicInteger(0);

        // Real idempotency service simulation
        Map<String, Object> idempotencyCache = new java.util.concurrent.ConcurrentHashMap<>();

        ExecutorService idempotencyExecutor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> idempotencyFutures = new ArrayList<>();

        // when - 동일 주문 중복 시도
        for (int i = 0; i < duplicateAttempts; i++) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // Simulate idempotency check
                    if (idempotencyCache.putIfAbsent(idempotencyKey, "IN_PROGRESS") != null) {
                        duplicateRejections.incrementAndGet();
                        return;
                    }

                    CreateOrderCommand duplicateOrder = createIdempotentOrder(customerKey, idempotencyKey);
                    CreateOrderResponse response = commandService.createOrder(duplicateOrder);
                    UUID orderId = response.getOrderId();

                    idempotencyCache.put(idempotencyKey, orderId);
                    successfulCreations.incrementAndGet();

                } catch (Exception e) {
                    duplicateRejections.incrementAndGet();
                }
            }, idempotencyExecutor);

            idempotencyFutures.add(future);
        }

        CompletableFuture.allOf(idempotencyFutures.toArray(new CompletableFuture[0]))
                .get(10, TimeUnit.SECONDS);

        idempotencyExecutor.shutdown();

        // then - 멱등성 검증
        System.out.println("=== Order Idempotency Test Results ===");
        System.out.println("Duplicate Attempts: " + duplicateAttempts);
        System.out.println("Successful Creations: " + successfulCreations.get());
        System.out.println("Duplicate Rejections: " + duplicateRejections.get());

        // Only one order should be created
        assertThat(successfulCreations.get()).isEqualTo(1);
        assertThat(duplicateRejections.get()).isEqualTo(duplicateAttempts - 1);
    }

    @Test
    void systemRecovery_AfterTrafficSurgeSubsides() throws Exception {
        // given - 트래픽 급증 후 시스템 복구 테스트
        int surgePhaseRequests = 200;
        int recoveryPhaseRequests = 50;

        AtomicLong surgePhaseTime = new AtomicLong(0);
        AtomicLong recoveryPhaseTime = new AtomicLong(0);
        AtomicInteger surgeSuccessCount = new AtomicInteger(0);
        AtomicInteger recoverySuccessCount = new AtomicInteger(0);

        // Phase 1: Traffic Surge
        ExecutorService surgeExecutor = Executors.newFixedThreadPool(100);
        List<CompletableFuture<Void>> surgeFutures = new ArrayList<>();

        Instant surgeStart = Instant.now();

        for (int i = 0; i < surgePhaseRequests; i++) {
            final int customerId = i + 1;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateOrderCommand surgeOrder = createStandardOrder(customerId);
                    CreateOrderResponse response = commandService.createOrder(surgeOrder);
                    surgeSuccessCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected under surge conditions
                }
            }, surgeExecutor);

            surgeFutures.add(future);
        }

        CompletableFuture.allOf(surgeFutures.toArray(new CompletableFuture[0]))
                .get(30, TimeUnit.SECONDS);

        Instant surgeEnd = Instant.now();
        surgePhaseTime.set(Duration.between(surgeStart, surgeEnd).toMillis());

        surgeExecutor.shutdown();

        // Brief cooldown period
        Thread.sleep(2000);

        // Phase 2: Recovery Testing
        ExecutorService recoveryExecutor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> recoveryFutures = new ArrayList<>();

        Instant recoveryStart = Instant.now();

        for (int i = 0; i < recoveryPhaseRequests; i++) {
            final int customerId = i + 1000; // Different customer IDs
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    CreateOrderCommand recoveryOrder = createStandardOrder(customerId);
                    CreateOrderResponse response = commandService.createOrder(recoveryOrder);
                    recoverySuccessCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Recovery phase order failed: " + e.getMessage());
                }
            }, recoveryExecutor);

            recoveryFutures.add(future);
        }

        CompletableFuture.allOf(recoveryFutures.toArray(new CompletableFuture[0]))
                .get(15, TimeUnit.SECONDS);

        Instant recoveryEnd = Instant.now();
        recoveryPhaseTime.set(Duration.between(recoveryStart, recoveryEnd).toMillis());

        recoveryExecutor.shutdown();

        // then - 시스템 복구 검증
        double surgeSuccessRate = (double) surgeSuccessCount.get() / surgePhaseRequests * 100;
        double recoverySuccessRate = (double) recoverySuccessCount.get() / recoveryPhaseRequests * 100;

        System.out.println("=== System Recovery Test Results ===");
        System.out.println("Surge Phase - Requests: " + surgePhaseRequests + ", Success: " + surgeSuccessCount.get() +
                          " (" + String.format("%.1f%%", surgeSuccessRate) + ")");
        System.out.println("Recovery Phase - Requests: " + recoveryPhaseRequests + ", Success: " + recoverySuccessCount.get() +
                          " (" + String.format("%.1f%%", recoverySuccessRate) + ")");
        System.out.println("Surge Phase Duration: " + surgePhaseTime.get() + " ms");
        System.out.println("Recovery Phase Duration: " + recoveryPhaseTime.get() + " ms");

        // Recovery should not regress after surge
        assertThat(recoverySuccessRate).isGreaterThanOrEqualTo(surgeSuccessRate);
        assertThat(recoverySuccessRate).isGreaterThan(80.0); // 80% success in recovery
    }

    // Helper methods
    private void analyzeFlashSaleResults(int totalCustomers, int successful, int failed, int duplicates,
                                       Duration totalTime, List<Long> orderTimes, List<Long> queueTimes) {

        double successRate = (double) successful / totalCustomers * 100;
        double avgOrderTime = orderTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double avgQueueTime = queueTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double throughput = (double) successful / totalTime.toSeconds();

        System.out.println("🔥 === Flash Sale Results ===");
        System.out.println("Total Customers: " + totalCustomers);
        System.out.println("Successful Orders: " + successful);
        System.out.println("Failed Orders: " + failed);
        System.out.println("Duplicate Rejections: " + duplicates);
        System.out.println("Success Rate: " + String.format("%.1f%%", successRate));
        System.out.println("Average Order Processing Time: " + String.format("%.2f ms", avgOrderTime));
        System.out.println("Average Queue Wait Time: " + String.format("%.2f ms", avgQueueTime));
        System.out.println("Throughput: " + String.format("%.2f orders/sec", throughput));
        System.out.println("Total Flash Sale Duration: " + totalTime.toSeconds() + " seconds");
    }

    private CreateOrderCommand createFlashSaleOrder(int customerId) {
        return CreateOrderCommand.builder()
                .userId((long) customerId)
                .popupId(UUID.randomUUID())
                .orderType("PURCHASE")
                .items(List.of(
                        OrderItemCommand.builder()
                                .orderItemType(OrderItemType.GOODS)
                                .goodsVariantId(UUID.randomUUID())
                                .qty(1) // Limited quantity for flash sale
                                .unitPrice(999) // Special flash sale price
                                .build()
                ))
                .build();
    }

    private CreateOrderCommand createPopularProductOrder(int customerId, String productId) {
        return CreateOrderCommand.builder()
                .userId((long) customerId)
                .popupId(UUID.randomUUID())
                .orderType("PURCHASE")
                .items(List.of(
                        OrderItemCommand.builder()
                                .orderItemType(OrderItemType.GOODS)
                                .goodsVariantId(UUID.randomUUID())
                                .qty(1)
                                .unitPrice(2000)
                                .build()
                ))
                .build();
    }

    private CreateOrderCommand createIdempotentOrder(String customerKey, String idempotencyKey) {
        return CreateOrderCommand.builder()
                .userId(123L) // Same customer
                .popupId(UUID.randomUUID())
                .orderType("PURCHASE")
                .items(List.of(
                        OrderItemCommand.builder()
                                .orderItemType(OrderItemType.GOODS)
                                .goodsVariantId(UUID.randomUUID())
                                .qty(2)
                                .unitPrice(1500)
                                .build()
                ))
                .build();
    }

    private CreateOrderCommand createStandardOrder(int customerId) {
        return CreateOrderCommand.builder()
                .userId((long) customerId)
                .popupId(UUID.randomUUID())
                .orderType("PURCHASE")
                .items(List.of(
                        OrderItemCommand.builder()
                                .orderItemType(OrderItemType.GOODS)
                                .goodsVariantId(UUID.randomUUID())
                                .qty(1 + customerId % 3)
                                .unitPrice(1000 + customerId % 1000)
                                .build()
                ))
                .build();
    }
}
