package com.popcorn.demo.domain.manager.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.popcorn.common.dto.BaseResponse;
import com.popcorn.demo.domain.manager.exception.OrderMicroserviceException;
import com.popcorn.order.dto.request.OrderCancelRequest;
import com.popcorn.order.dto.request.OrderStatusUpdateRequest;
import com.popcorn.order.dto.response.OrderCancelResponse;
import com.popcorn.order.dto.response.OrderDetailResponse;
import com.popcorn.order.dto.response.OrderListResponse;
import com.popcorn.order.dto.response.OrderStatusUpdateResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Order 마이크로서비스 HTTP 클라이언트
 *
 * Resilience4j를 사용한 Circuit Breaker, Retry, TimeLimiter 적용
 * Manager 서비스에서 Order 마이크로서비스 호출 시 사용
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMicroserviceClient {

    private final RestTemplate restTemplate;

    @Value("${microservices.order.base-url:http://localhost:8082}")
    private String orderServiceBaseUrl;

    /**
     * 주문 취소 요청
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "cancelOrderFallback")
    @Retry(name = "orderService")
    @TimeLimiter(name = "orderService")
    public OrderCancelResponse cancelOrder(OrderCancelRequest request) {
        try {
            log.info("🌐 Order 마이크로서비스 호출 - 주문 취소: popupId={}", request.getPopupId());

            String url = orderServiceBaseUrl + "/api/orders/cancel";
            BaseResponse<OrderCancelResponse> response = restTemplate.postForObject(
                url, request, BaseResponse.class);

            if (response != null && response.getData() != null) {
                log.info("✅ Order 마이크로서비스 응답 성공 - 주문 취소");
                return (OrderCancelResponse) response.getData();
            } else {
                log.warn("⚠️ Order 마이크로서비스 응답이 비어있음");
                throw new RuntimeException("Order 마이크로서비스 응답이 올바르지 않습니다");
            }

        } catch (HttpClientErrorException e) {
            log.error("❌ Order 마이크로서비스 클라이언트 오류 - 주문 취소: {}", e.getStatusCode());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new OrderMicroserviceException.BusinessException("취소할 주문을 찾을 수 없습니다");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new OrderMicroserviceException.BusinessException("잘못된 취소 요청입니다");
            }
            throw new OrderMicroserviceException("주문 취소 요청이 거절되었습니다: " + e.getStatusText(), e);
        } catch (HttpServerErrorException e) {
            log.error("❌ Order 마이크로서비스 서버 오류 - 주문 취소: {}", e.getStatusCode());
            throw new OrderMicroserviceException("Order 마이크로서비스 내부 오류가 발생했습니다", e);
        } catch (ResourceAccessException e) {
            log.error("❌ Order 마이크로서비스 연결 실패 - 주문 취소", e);
            throw new OrderMicroserviceException.TimeoutException("주문 취소");
        } catch (Exception e) {
            log.error("❌ Order 마이크로서비스 호출 실패 - 주문 취소", e);
            throw new OrderMicroserviceException("주문 취소 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 상태 변경 요청
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "updateOrderStatusFallback")
    @Retry(name = "orderService")
    @TimeLimiter(name = "orderService")
    public OrderStatusUpdateResponse updateOrderStatus(UUID popupId, OrderStatusUpdateRequest request) {
        try {
            log.info("🌐 Order 마이크로서비스 호출 - 상태 변경: popupId={}, status={}",
                popupId, request.getStatus());

            String url = orderServiceBaseUrl + "/api/orders/" + popupId + "/status";
            BaseResponse<OrderStatusUpdateResponse> response = restTemplate.patchForObject(
                url, request, BaseResponse.class);

            if (response != null && response.getData() != null) {
                log.info("✅ Order 마이크로서비스 응답 성공 - 상태 변경");
                return (OrderStatusUpdateResponse) response.getData();
            } else {
                throw new RuntimeException("Order 마이크로서비스 응답이 올바르지 않습니다");
            }

        } catch (Exception e) {
            log.error("❌ Order 마이크로서비스 호출 실패 - 상태 변경", e);
            throw new RuntimeException("주문 상태 변경 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 상세 조회
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderDetailFallback")
    @Retry(name = "orderService")
    @TimeLimiter(name = "orderService")
    public OrderDetailResponse getOrderDetail(UUID popupId) {
        try {
            log.info("🌐 Order 마이크로서비스 호출 - 상세 조회: popupId={}", popupId);

            String url = orderServiceBaseUrl + "/api/orders/" + popupId;
            BaseResponse<OrderDetailResponse> response = restTemplate.getForObject(
                url, BaseResponse.class);

            if (response != null && response.getData() != null) {
                log.info("✅ Order 마이크로서비스 응답 성공 - 상세 조회");
                return (OrderDetailResponse) response.getData();
            } else {
                throw new RuntimeException("Order 마이크로서비스 응답이 올바르지 않습니다");
            }

        } catch (Exception e) {
            log.error("❌ Order 마이크로서비스 호출 실패 - 상세 조회", e);
            throw new RuntimeException("주문 상세 조회 요청 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문 목록 조회
     */
    @CircuitBreaker(name = "orderService", fallbackMethod = "getOrderListFallback")
    @Retry(name = "orderService")
    @TimeLimiter(name = "orderService")
    public OrderListResponse getOrderList(String status, Integer page, Integer size) {
        try {
            log.info("🌐 Order 마이크로서비스 호출 - 목록 조회: status={}, page={}", status, page);

            String url = String.format("%s/api/orders?status=%s&page=%d&size=%d",
                orderServiceBaseUrl, status, page, size);
            BaseResponse<OrderListResponse> response = restTemplate.getForObject(
                url, BaseResponse.class);

            if (response != null && response.getData() != null) {
                log.info("✅ Order 마이크로서비스 응답 성공 - 목록 조회");
                return (OrderListResponse) response.getData();
            } else {
                throw new RuntimeException("Order 마이크로서비스 응답이 올바르지 않습니다");
            }

        } catch (Exception e) {
            log.error("❌ Order 마이크로서비스 호출 실패 - 목록 조회", e);
            throw new RuntimeException("주문 목록 조회 요청 실패: " + e.getMessage(), e);
        }
    }

    // ========================= Fallback Methods =========================

    public OrderCancelResponse cancelOrderFallback(OrderCancelRequest request, Exception e) {
        log.error("🔥 Circuit Breaker - Order 취소 Fallback 실행: {}", e.getMessage());
        return OrderCancelResponse.builder()
            .popupId(request.getPopupId())
            .reason("서비스 일시 중단으로 인한 처리 지연")
            .build();
    }

    public OrderStatusUpdateResponse updateOrderStatusFallback(UUID popupId, OrderStatusUpdateRequest request, Exception e) {
        log.error("🔥 Circuit Breaker - Order 상태변경 Fallback 실행: {}", e.getMessage());
        return OrderStatusUpdateResponse.builder()
            .popupId(popupId)
            .status(request.getStatus())
            .build();
    }

    public OrderDetailResponse getOrderDetailFallback(UUID popupId, Exception e) {
        log.error("🔥 Circuit Breaker - Order 상세조회 Fallback 실행: {}", e.getMessage());
        return OrderDetailResponse.builder()
            .popupId(popupId)
            .title("서비스 일시 중단")
            .description("현재 주문 상세 정보를 불러올 수 없습니다")
            .build();
    }

    public OrderListResponse getOrderListFallback(String status, Integer page, Integer size, Exception e) {
        log.error("🔥 Circuit Breaker - Order 목록조회 Fallback 실행: {}", e.getMessage());
        return OrderListResponse.builder()
            .items(java.util.Collections.emptyList())
            .page(page)
            .size(size)
            .total(0L)
            .build();
    }

}