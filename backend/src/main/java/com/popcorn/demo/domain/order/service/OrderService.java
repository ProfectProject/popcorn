package com.popcorn.demo.domain.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 주문 비동기 처리 서비스
 *
 * 주요 기능:
 * - 주문 후처리 작업 비동기 실행
 * - 외부 시스템 통신 비동기 처리
 * - 이벤트 발행 및 알림 처리
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final Executor validationExecutor;

    public OrderService(@Qualifier("orderValidationTaskExecutor") Executor validationExecutor) {
        this.validationExecutor = validationExecutor;
    }

    /**
     * 주문 생성 후처리 작업
     * - 재고 차감
     * - 알림 발송
     * - 이벤트 발행
     *
     * @param orderId 주문 ID
     * @return 처리 결과를 담은 CompletableFuture
     */
    @Async("orderTaskExecutor")
    public CompletableFuture<Void> processOrderPostActions(Long orderId) {
        try {
            // 1. 재고 차감 (시뮬레이션)
            Thread.sleep(100);
            log.info("주문 {} 재고 차감 완료", orderId);

            // 2. 알림 발송 (시뮬레이션)
            Thread.sleep(50);
            log.info("주문 {} 고객 알림 발송 완료", orderId);

            // 3. 이벤트 발행 (시뮬레이션)
            Thread.sleep(30);
            log.info("주문 {} 이벤트 발행 완료", orderId);

            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 주문 검증 비동기 처리
     * - 상품 재고 확인
     * - 고객 신용도 확인
     * - 프로모션 유효성 확인
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @param qty 수량
     * @return 검증 결과를 담은 CompletableFuture
     */
    @Async("orderValidationTaskExecutor")
    public CompletableFuture<Boolean> validateOrderAsync(Long userId, Long productId, Integer qty) {
        CompletableFuture<Boolean> stockFuture = CompletableFuture.supplyAsync(
                () -> validateStock(qty), validationExecutor
        );
        CompletableFuture<Boolean> userFuture = CompletableFuture.supplyAsync(
                () -> validateCustomer(userId), validationExecutor
        );
        CompletableFuture<Boolean> productFuture = CompletableFuture.supplyAsync(
                () -> validateProduct(productId), validationExecutor
        );

        return CompletableFuture.allOf(stockFuture, userFuture, productFuture)
                .thenApply(ignored -> stockFuture.join() && userFuture.join() && productFuture.join())
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("주문 검증 실패 - 사용자: {}, 상품: {}, 에러: {}", userId, productId, throwable.getMessage());
                    } else {
                        log.info("주문 검증 완료 - 사용자: {}, 상품: {}, 결과: {}", userId, productId, result);
                    }
                });
    }

    /**
     * 결제 처리 비동기 시뮬레이션
     * - 외부 결제 게이트웨이 호출
     * - 결제 결과 처리
     *
     * @param orderId 주문 ID
     * @param amount 결제 금액
     * @return 결제 결과를 담은 CompletableFuture
     */
    @Async("orderTaskExecutor")
    public CompletableFuture<String> processPaymentAsync(Long orderId, Integer amount) {
        try {
            // 외부 결제 게이트웨이 호출 시뮬레이션
            Thread.sleep(200);

            // 성공률 90% 시뮬레이션
            boolean paymentSuccess = Math.random() > 0.1;

            String paymentId = paymentSuccess ? "PAY-" + System.currentTimeMillis() : null;
            log.info("주문 {} 결제 처리 {}", orderId,
                    paymentSuccess ? "성공: " + paymentId : "실패");

            return CompletableFuture.completedFuture(paymentId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }

    private boolean validateStock(Integer qty) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("stock validation interrupted", e);
        }
        return qty != null && qty > 0 && qty <= 100;
    }

    private boolean validateCustomer(Long userId) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("customer validation interrupted", e);
        }
        return userId != null && userId > 0;
    }

    private boolean validateProduct(Long productId) {
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("product validation interrupted", e);
        }
        return productId != null && productId > 0;
    }
}
