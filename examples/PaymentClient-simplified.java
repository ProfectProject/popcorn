// Istio 도입 후 단순화된 PaymentClient 예시
// order/src/main/java/com/popcorn/order/client/PaymentClient.java

package com.popcorn.order.client;

import com.popcorn.order.dto.PaymentRequest;
import com.popcorn.order.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class PaymentClient {
    
    private final WebClient paymentWebClient;
    
    public PaymentClient(@Qualifier("paymentWebClient") WebClient paymentWebClient) {
        this.paymentWebClient = paymentWebClient;
    }
    
    /**
     * 결제 생성
     * Istio가 Circuit Breaker, Retry, Timeout을 처리하므로 애노테이션 제거
     */
    // @CircuitBreaker(name = "payment", fallbackMethod = "fallbackCreatePayment") // 제거
    // @Retry(name = "payment") // 제거
    // @RateLimiter(name = "order") // 제거
    public Mono<PaymentResponse> createPayment(PaymentRequest request) {
        return paymentWebClient
                .post()
                .uri("/api/payments/v1/payments")
                .bodyValue(request)
                // 내부 서비스 호출 헤더 제거 (mTLS로 자동 인증)
                // .header("X-Internal-Call", "true") // 제거
                // .header("X-Internal-Service", "order") // 제거
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnSuccess(response -> log.info("Payment created successfully: {}", response.getPaymentId()))
                .doOnError(error -> log.error("Payment creation failed", error));
    }
    
    /**
     * 결제 조회
     */
    public Mono<PaymentResponse> getPayment(String paymentId) {
        return paymentWebClient
                .get()
                .uri("/api/payments/v1/payments/{paymentId}", paymentId)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnSuccess(response -> log.debug("Payment retrieved: {}", paymentId))
                .doOnError(error -> log.error("Payment retrieval failed for ID: {}", paymentId, error));
    }
    
    /**
     * 결제 취소
     */
    public Mono<PaymentResponse> cancelPayment(String paymentId, String reason) {
        return paymentWebClient
                .post()
                .uri("/api/payments/v1/payments/{paymentId}/cancel", paymentId)
                .bodyValue(Map.of("reason", reason))
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnSuccess(response -> log.info("Payment cancelled: {}", paymentId))
                .doOnError(error -> log.error("Payment cancellation failed for ID: {}", paymentId, error));
    }
    
    // Fallback 메서드 제거 (Istio Circuit Breaker가 처리)
    /*
    public Mono<PaymentResponse> fallbackCreatePayment(PaymentRequest request, Exception ex) {
        log.warn("Payment service fallback triggered", ex);
        return Mono.just(PaymentResponse.builder()
                .status("PENDING")
                .message("Payment service temporarily unavailable. Please try again later.")
                .build());
    }
    */
}