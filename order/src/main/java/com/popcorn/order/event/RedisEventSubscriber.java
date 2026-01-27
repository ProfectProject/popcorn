package com.popcorn.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Redis Pub/Sub을 사용한 이벤트 구독자
 *
 * Store 서비스에서 발행한 재고 처리 결과 이벤트를 수신
 * 나중에 Kafka Consumer로 전환할 때는 이 클래스만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.info("Redis 이벤트 수신 - channel: {}, body: {}", channel, body);

            if ("events:stock-deduction-success".equals(channel)) {
                handleStockDeductionSuccess(body);
            } else if ("events:stock-deduction-failed".equals(channel)) {
                handleStockDeductionFailed(body);
            }

        } catch (Exception e) {
            log.error("Redis 이벤트 처리 중 오류 발생 - message: {}, error: {}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 성공 이벤트 처리
     */
    private void handleStockDeductionSuccess(String eventJson) {
        try {
            log.info("재고 차감 성공 이벤트 처리 시작 - eventJson: {}", eventJson);

            String resolvedJson = resolveEventJson(eventJson);
            StockDeductionSuccessEventDto eventDto = objectMapper.readValue(
                    resolvedJson, StockDeductionSuccessEventDto.class);

            // DTO를 내부 이벤트 객체로 변환
            StockDeductionSuccessEvent event = StockDeductionSuccessEvent.create(
                    eventDto.getOrderId(),
                    eventDto.getOrderNo(),
                    eventDto.getPopupId(),
                    eventDto.getStockDetails()
            );

            // Spring 내부 이벤트로 발행 (기존 handleStockDeductionSuccess가 받음)
            applicationEventPublisher.publishEvent(event);

            log.info("재고 차감 성공 이벤트 처리 완료 - orderId: {}, eventId: {}",
                    eventDto.getOrderId(), eventDto.getEventId());

        } catch (Exception e) {
            log.error("재고 차감 성공 이벤트 처리 중 오류 - eventJson: {}, error: {}",
                    eventJson, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 처리
     */
    private void handleStockDeductionFailed(String eventJson) {
        try {
            log.info("재고 차감 실패 이벤트 처리 시작 - eventJson: {}", eventJson);

            String resolvedJson = resolveEventJson(eventJson);
            StockDeductionFailedEventDto eventDto = objectMapper.readValue(
                    resolvedJson, StockDeductionFailedEventDto.class);

            // DTO를 내부 이벤트 객체로 변환
            StockDeductionFailedEvent event = StockDeductionFailedEvent.forSystemError(
                    eventDto.getOrderId(),
                    eventDto.getOrderNo(),
                    eventDto.getReason()
            );

            // Spring 내부 이벤트로 발행 (기존 handleInternalStockDeductionFailed가 받음)
            applicationEventPublisher.publishEvent(event);

            log.info("재고 차감 실패 이벤트 처리 완료 - orderId: {}, eventId: {}",
                    eventDto.getOrderId(), eventDto.getEventId());

        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 처리 중 오류 - eventJson: {}, error: {}",
                    eventJson, e.getMessage(), e);
        }
    }

    // DTO classes for Redis deserialization

    private String resolveEventJson(String eventJson) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(eventJson);
            if (node != null && node.isTextual()) {
                return node.asText();
            }
        } catch (Exception ignored) {
        }
        return eventJson;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockDeductionSuccessEventDto {
        private String eventId;
        private UUID orderId;
        private String orderNo;
        private UUID popupId;
        private String stockDetails;
        private LocalDateTime succeededAt;

        public StockDeductionSuccessEventDto() {}

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StockDeductionFailedEventDto {
        private String eventId;
        private UUID orderId;
        private String orderNo;
        private String reason;
        private String failureCode;
        private LocalDateTime failedAt;

        public StockDeductionFailedEventDto() {}

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
}
