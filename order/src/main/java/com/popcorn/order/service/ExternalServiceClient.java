package com.popcorn.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalServiceClient {

    private final CircuitBreakerFactory circuitBreakerFactory;
    private final WebClient.Builder webClientBuilder;

    @Value("${microservices.payment.base-url}")
    private String paymentBaseUrl;

    @Value("${microservices.user.base-url}")
    private String userBaseUrl;

    @Value("${microservices.inventory.base-url:${microservices.gateway.base-url:http://localhost:8080}}")
    private String inventoryBaseUrl;


    public CompletableFuture<Map<String, Object>> processPayment(UUID orderId, Long amount, String paymentMethod) {
        CircuitBreaker paymentCircuitBreaker = circuitBreakerFactory.create("payment-service");

        return CompletableFuture.supplyAsync(() ->
            paymentCircuitBreaker.run(
        
                () -> {
                    log.info("결제 서비스 호출 시작 - 주문ID: {}, 금액: {}", orderId, amount);

                    WebClient webClient = webClientBuilder.build();
                    Map<String, Object> paymentRequest = Map.of(
                        "orderId", orderId.toString(),
                        "amount", amount,
                        "paymentMethod", paymentMethod
                    );

                    Map<String, Object> response = webClient.post()
                        .uri(paymentBaseUrl + "/api/pay/v1/payments/process")
                        .bodyValue(paymentRequest)
                        .headers(headers -> {
                            String authHeader = resolveAuthHeader();
                            if (authHeader != null) {
                                headers.set("Authorization", authHeader);
                            }
                        })
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(5));

                    log.info("결제 서비스 응답 성공 - 주문ID: {}", orderId);
                    return response;
                },
     
                throwable -> {
                    log.warn("결제 서비스 장애 발생 - 폴백 실행. 주문ID: {}, 오류: {}",
                        orderId, throwable.getMessage());

                 
                    return Map.of(
                        "status", "PENDING",
                        "orderId", orderId.toString(),
                        "message", "결제 서비스 일시 장애로 결제가 보류되었습니다.",
                        "retryable", true,
                        "fallbackReason", throwable.getMessage()
                    );
                }
            )
        );
    }

    // ========================= 사용자 서비스 호출 =========================

  
    public CompletableFuture<Map<String, Object>> getUserInfo(Long userId) {
        CircuitBreaker userCircuitBreaker = circuitBreakerFactory.create("user-service");

        return CompletableFuture.supplyAsync(() ->
            userCircuitBreaker.run(
                () -> {
                    log.info("사용자 서비스 호출 시작 - 사용자ID: {}", userId);

                    WebClient webClient = webClientBuilder.build();
                    Map<String, Object> response = webClient.get()
                        .uri(userBaseUrl + "/api/users/v1/{userId}", userId)
                        .headers(headers -> {
                            String authHeader = resolveAuthHeader();
                            if (authHeader != null) {
                                headers.set("Authorization", authHeader);
                            }
                        })
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(3));

                    log.info("사용자 서비스 응답 성공 - 사용자ID: {}", userId);
                    return response;
                },
                throwable -> {
                    log.warn("사용자 서비스 장애 발생 - 폴백 실행. 사용자ID: {}, 오류: {}",
                        userId, throwable.getMessage());

                    // 기본 사용자 정보로 폴백 (캐시에서 가져올 수도 있음)
                    return Map.of(
                        "userId", userId,
                        "name", "사용자" + userId, // 임시 이름
                        "email", "user" + userId + "@temp.com",
                        "status", "FALLBACK",
                        "message", "사용자 서비스 장애로 기본 정보를 제공합니다.",
                        "fallbackReason", throwable.getMessage()
                    );
                }
            )
        );
    }

    // ========================= 재고 서비스 호출 =========================

  
    public CompletableFuture<Map<String, Object>> checkAndReserveStock(UUID orderId, Long productId, Integer quantity) {
        CircuitBreaker inventoryCircuitBreaker = circuitBreakerFactory.create("inventory-service");

        return CompletableFuture.supplyAsync(() ->
            inventoryCircuitBreaker.run(
                () -> {
                    log.info("재고 서비스 호출 시작 - 주문ID: {}, 상품ID: {}, 수량: {}",
                        orderId, productId, quantity);

                    WebClient webClient = webClientBuilder.build();
                    Map<String, Object> stockRequest = Map.of(
                        "orderId", orderId.toString(),
                        "productId", productId,
                        "quantity", quantity
                    );

                    Map<String, Object> response = webClient.post()
                        .uri(inventoryBaseUrl + "/api/inventory/v1/reserve")
                        .bodyValue(stockRequest)
                        .headers(headers -> {
                            String authHeader = resolveAuthHeader();
                            if (authHeader != null) {
                                headers.set("Authorization", authHeader);
                            }
                        })
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block(Duration.ofSeconds(4));

                    log.info("재고 서비스 응답 성공 - 주문ID: {}", orderId);
                    return response;
                },
                throwable -> {
                    log.error("재고 서비스 장애 발생 - 폴백 실행. 주문ID: {}, 오류: {}",
                        orderId, throwable.getMessage());

                    // 재고 서비스 장애시 안전하게 재고 부족으로 처리
                    return Map.of(
                        "status", "INSUFFICIENT_STOCK",
                        "available", false,
                        "orderId", orderId.toString(),
                        "productId", productId,
                        "requestedQuantity", quantity,
                        "message", "재고 서비스 장애로 재고 확인이 불가능합니다.",
                        "fallbackReason", throwable.getMessage(),
                        "action", "RETRY_LATER"
                    );
                }
            )
        );
    }

  
    public CompletableFuture<Map<String, Object>> processOrderWithCircuitBreaker(
            UUID orderId, Long userId, Long productId, Integer quantity, Long amount, String paymentMethod) {

        log.info("통합 주문 처리 시작 - 주문ID: {}", orderId);

        // 1. 사용자 정보 조회 (병렬 실행)
        CompletableFuture<Map<String, Object>> userFuture = getUserInfo(userId);

        // 2. 재고 확인 및 차감 (병렬 실행)
        CompletableFuture<Map<String, Object>> stockFuture = checkAndReserveStock(orderId, productId, quantity);

        // 3. 사용자 정보와 재고 확인 완료 후 결제 진행
        return CompletableFuture.allOf(userFuture, stockFuture)
            .thenCompose(v -> {
                try {
                    Map<String, Object> userInfo = userFuture.get();
                    Map<String, Object> stockInfo = stockFuture.get();

                    // 재고 확인 성공한 경우에만 결제 진행
                    if (Boolean.TRUE.equals(stockInfo.get("available"))) {
                        log.info("재고 확인 성공 - 결제 진행. 주문ID: {}", orderId);
                        return processPayment(orderId, amount, paymentMethod);
                    } else {
                        log.warn("재고 부족으로 주문 실패. 주문ID: {}", orderId);
                        return CompletableFuture.completedFuture(Map.of(
                            "status", "FAILED",
                            "reason", "INSUFFICIENT_STOCK",
                            "stockInfo", stockInfo
                        ));
                    }
                } catch (Exception e) {
                    log.error("통합 주문 처리 중 오류 발생. 주문ID: {}", orderId, e);
                    return CompletableFuture.completedFuture(Map.of(
                        "status", "ERROR",
                        "message", "주문 처리 중 오류가 발생했습니다.",
                        "error", e.getMessage()
                    ));
                }
            });
    }


    public Map<String, String> getCircuitBreakerStatuses() {
        return Map.of(
            "payment-service", getCircuitBreakerStatus("payment-service"),
            "user-service", getCircuitBreakerStatus("user-service"),
            "inventory-service", getCircuitBreakerStatus("inventory-service")
        );
    }

    private String getCircuitBreakerStatus(String serviceName) {
        try {
            CircuitBreaker cb = circuitBreakerFactory.create(serviceName);
            return "MONITORING"; // 실제로는 Resilience4j의 상태 조회 API 사용
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String resolveAuthHeader() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null || attributes.getRequest() == null) {
            return null;
        }
        return attributes.getRequest().getHeader("Authorization");
    }
}
