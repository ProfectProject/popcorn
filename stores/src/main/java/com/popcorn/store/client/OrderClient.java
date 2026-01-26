package com.popcorn.store.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Order 마이크로서비스와 통신하는 HTTP Client
 *
 * 재고 처리 결과를 Order 서비스로 이벤트 전송
 * 나중에 Kafka Producer로 교체 예정
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${microservices.order.base-url:http://localhost:8080}")
    private String orderBaseUrl;

    private WebClient orderWebClient() {
        return webClientBuilder.baseUrl(orderBaseUrl).build();
    }

    /**
     * 재고 차감 성공 이벤트를 Order 서비스로 전송
     *
     * @param orderId 주문 ID
     * @param orderNo 주문 번호
     * @param popupId 팝업 ID
     * @param stockDetails 차감된 재고 정보
     */
    public void sendStockDeductionSuccessEvent(UUID orderId, String orderNo, UUID popupId, String stockDetails) {
        try {
            log.info("재고 차감 성공 이벤트 전송 시작 - orderId: {}", orderId);

            StockDeductionSuccessEventDto event = StockDeductionSuccessEventDto.create(
                    orderId, orderNo, popupId, stockDetails);

            // HTTP POST로 Order 서비스에 이벤트 전송 (나중에 Kafka Producer로 교체)
            BaseResponse<Object> response = orderWebClient()
                    .post()
                    .uri("/api/orders/v1/events/stock-deduction-success")
                    .headers(headers -> headers.set("Content-Type", "application/json"))
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                    .block();

            if (response == null || response.getCode() != 200) {
                String message = response != null ? response.getMessage() : "Order 서비스 응답이 비어 있습니다.";
                log.error("재고 차감 성공 이벤트 전송 실패 - orderId: {}, message: {}", orderId, message);
            } else {
                log.info("재고 차감 성공 이벤트 전송 완료 - orderId: {}, eventId: {}",
                        orderId, event.getEventId());
            }

        } catch (Exception e) {
            log.error("재고 차감 성공 이벤트 전송 중 오류 - orderId: {}, error: {}",
                    orderId, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트를 Order 서비스로 전송
     *
     * @param orderId 주문 ID
     * @param orderNo 주문 번호
     * @param reason 실패 이유
     */
    public void sendStockDeductionFailedEvent(UUID orderId, String orderNo, String reason) {
        try {
            log.info("재고 차감 실패 이벤트 전송 시작 - orderId: {}, reason: {}", orderId, reason);

            StockDeductionFailedEventDto event = StockDeductionFailedEventDto.forSystemError(orderId, orderNo, reason);

            // HTTP POST로 Order 서비스에 이벤트 전송 (나중에 Kafka Producer로 교체)
            BaseResponse<Object> response = orderWebClient()
                    .post()
                    .uri("/api/orders/v1/events/stock-deduction-failed")
                    .headers(headers -> headers.set("Content-Type", "application/json"))
                    .bodyValue(event)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<BaseResponse<Object>>() {})
                    .block();

            if (response == null || response.getCode() != 200) {
                String message = response != null ? response.getMessage() : "Order 서비스 응답이 비어 있습니다.";
                log.error("재고 차감 실패 이벤트 전송 실패 - orderId: {}, message: {}", orderId, message);
            } else {
                log.info("재고 차감 실패 이벤트 전송 완료 - orderId: {}, eventId: {}",
                        orderId, event.getEventId());
            }

        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 전송 중 오류 - orderId: {}, error: {}",
                    orderId, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 성공 이벤트 DTO
     */
    public static class StockDeductionSuccessEventDto {
        private String eventId;
        private UUID orderId;
        private String orderNo;
        private UUID popupId;
        private String stockDetails;
        private LocalDateTime succeededAt;

        public StockDeductionSuccessEventDto() {}

        public StockDeductionSuccessEventDto(String eventId, UUID orderId, String orderNo,
                                           UUID popupId, String stockDetails, LocalDateTime succeededAt) {
            this.eventId = eventId;
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.popupId = popupId;
            this.stockDetails = stockDetails;
            this.succeededAt = succeededAt;
        }

        public static StockDeductionSuccessEventDto create(UUID orderId, String orderNo,
                                                         UUID popupId, String stockDetails) {
            return new StockDeductionSuccessEventDto(
                    UUID.randomUUID().toString(),
                    orderId,
                    orderNo,
                    popupId,
                    stockDetails,
                    LocalDateTime.now()
            );
        }

        // getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

        public UUID getPopupId() { return popupId; }
        public void setPopupId(UUID popupId) { this.popupId = popupId; }

        public String getStockDetails() { return stockDetails; }
        public void setStockDetails(String stockDetails) { this.stockDetails = stockDetails; }

        public LocalDateTime getSucceededAt() { return succeededAt; }
        public void setSucceededAt(LocalDateTime succeededAt) { this.succeededAt = succeededAt; }
    }

    /**
     * 재고 차감 실패 이벤트 DTO
     */
    public static class StockDeductionFailedEventDto {
        private String eventId;
        private UUID orderId;
        private String orderNo;
        private String reason;
        private String failureCode;
        private LocalDateTime failedAt;

        public StockDeductionFailedEventDto() {}

        public StockDeductionFailedEventDto(String eventId, UUID orderId, String orderNo,
                                          String reason, String failureCode, LocalDateTime failedAt) {
            this.eventId = eventId;
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.reason = reason;
            this.failureCode = failureCode;
            this.failedAt = failedAt;
        }

        public static StockDeductionFailedEventDto forSystemError(UUID orderId, String orderNo, String reason) {
            return new StockDeductionFailedEventDto(
                    UUID.randomUUID().toString(),
                    orderId,
                    orderNo,
                    reason,
                    "SYSTEM_ERROR",
                    LocalDateTime.now()
            );
        }

        // getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getFailureCode() { return failureCode; }
        public void setFailureCode(String failureCode) { this.failureCode = failureCode; }

        public LocalDateTime getFailedAt() { return failedAt; }
        public void setFailedAt(LocalDateTime failedAt) { this.failedAt = failedAt; }
    }

    /**
     * API 응답 공통 DTO
     */
    public static class BaseResponse<T> {
        private int code;
        private String message;
        private T data;

        public BaseResponse() {}

        // getters and setters
        public int getCode() { return code; }
        public void setCode(int code) { this.code = code; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
}