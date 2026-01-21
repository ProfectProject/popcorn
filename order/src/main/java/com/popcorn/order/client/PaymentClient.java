package com.popcorn.order.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.popcorn.order.dto.payment.CreatePaymentRequest;
import com.popcorn.order.dto.payment.CreatePaymentResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Payment 마이크로서비스와 통신하는 HTTP Client
 *
 * - WebClient: 비동기 HTTP 통신 (RestTemplate의 최신 버전)
 * - Circuit Breaker: 결제 서비스가 문제가 있을 때 빠르게 실패
 * - Retry: 일시적 네트워크 문제 시 자동 재시도
 * - Rate Limiter: 너무 많은 요청을 방지
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${microservices.payment.base-url}")
    private String paymentBaseUrl;

    /**
     * 주문에 대한 결제 생성하기
     *
     * [Circuit Breaker]
     * - 결제 서비스가 장애나면 빠르게 fallback으로 전환
     * - 60% 실패율에서 Circuit Open (5초간 차단)
     *
     * [Retry]
     * - 네트워크 일시 장애 시 최대 3번 재시도
     * - 2초씩 기다리면서 재시도 (지수 백오프)
     *
     * [Rate Limiter]
     * - 초당 최대 100개 요청만 허용
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "createPaymentFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService")
    public Mono<CreatePaymentResponse> createPayment(CreatePaymentRequest request) {
        log.info("결제 생성 요청 - 주문ID: {}, 금액: {}", request.getOrderId(), request.getAmount());

        return webClientBuilder.build()
                .post()
                .uri(paymentBaseUrl + "/api/payments")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(CreatePaymentResponse.class)
                .doOnSuccess(response ->
                    log.info("결제 생성 성공 - 주문ID: {}, 결제ID: {}",
                        request.getOrderId(), response.getPaymentId()))
                .doOnError(error ->
                    log.error("결제 생성 실패 - 주문ID: {}, 에러: {}",
                        request.getOrderId(), error.getMessage()));
    }

    /**
     * 결제 서비스 장애 시 Fallback 메서드
     *
     * [초보자 가이드]
     * - Circuit Breaker가 열렸을 때 호출됨
     * - 주문은 생성됐지만 결제는 나중에 처리하도록 함
     */
    public Mono<CreatePaymentResponse> createPaymentFallback(CreatePaymentRequest request, Exception ex) {
        log.warn("결제 서비스 장애로 Fallback 실행 - 주문ID: {}, 이유: {}",
            request.getOrderId(), ex.getMessage());

        // 나중에 처리할 수 있도록 결제 대기 상태로 응답
        CreatePaymentResponse fallbackResponse = CreatePaymentResponse.builder()
                .paymentId(UUID.randomUUID())
                .orderId(request.getOrderId())
                .status("PENDING")
                .message("결제 서비스 일시 장애로 나중에 처리됩니다")
                .build();

        return Mono.just(fallbackResponse);
    }

    /**
     * 결제 상태 조회하기
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "getPaymentStatusFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService")
    public Mono<String> getPaymentStatus(UUID paymentId) {
        log.debug("결제 상태 조회 - 결제ID: {}", paymentId);

        return webClientBuilder.build()
                .get()
                .uri(paymentBaseUrl + "/api/payments/{paymentId}/status", paymentId)
                .retrieve()
                .bodyToMono(String.class);
    }

    /**
     * 결제 상태 조회 Fallback
     */
    public Mono<String> getPaymentStatusFallback(UUID paymentId, Exception ex) {
        log.warn("결제 상태 조회 실패 - 결제ID: {}, Fallback 응답", paymentId);
        return Mono.just("UNKNOWN");
    }

    /**
     * 결제 취소하기
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "cancelPaymentFallback")
    @Retry(name = "paymentService")
    @RateLimiter(name = "paymentService")
    public Mono<Void> cancelPayment(UUID paymentId, String reason) {
        log.info("결제 취소 요청 - 결제ID: {}, 이유: {}", paymentId, reason);

        return webClientBuilder.build()
                .delete()
                .uri(paymentBaseUrl + "/api/payments/{paymentId}?reason={reason}", paymentId, reason)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(result ->
                    log.info("결제 취소 성공 - 결제ID: {}", paymentId))
                .doOnError(error ->
                    log.error("결제 취소 실패 - 결제ID: {}, 에러: {}", paymentId, error.getMessage()));
    }

    /**
     * 결제 취소 Fallback
     */
    public Mono<Void> cancelPaymentFallback(UUID paymentId, String reason, Exception ex) {
        log.error("결제 취소 Fallback - 결제ID: {}, 나중에 수동 처리 필요", paymentId);
        // 실제로는 별도 큐에 넣어서 나중에 처리하도록 해야 함
        return Mono.empty();
    }

}