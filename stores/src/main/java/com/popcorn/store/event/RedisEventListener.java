package com.popcorn.store.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.store.domain.goods.service.GoodsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Redis Pub/Sub을 사용한 이벤트 리스너
 *
 * Order 서비스에서 발행한 재고 관련 이벤트를 수신하여 처리
 * 나중에 Kafka Consumer로 전환할 때는 이 클래스만 교체하면 됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final GoodsService goodsService;
    private final RedisTemplate<String, Object> redisTemplate;

    // 이벤트 토픽 상수
    private static final String STOCK_DEDUCTION_SUCCESS_TOPIC = "events:stock-deduction-success";
    private static final String STOCK_DEDUCTION_FAILED_TOPIC = "events:stock-deduction-failed";

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.info("Redis 이벤트 수신 - channel: {}, body: {}", channel, body);

            if ("events:stock-deduction-requested".equals(channel)) {
                handleStockDeductionRequested(body);
            }

        } catch (Exception e) {
            log.error("Redis 이벤트 처리 중 오류 발생 - message: {}, error: {}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 요청 이벤트 처리
     */
    private void handleStockDeductionRequested(String eventJson) {
        try {
            log.info("재고 차감 요청 이벤트 처리 시작 - eventJson: {}", eventJson);

            StockDeductionRequestedEventDto event = objectMapper.readValue(
                    eventJson, StockDeductionRequestedEventDto.class);

            log.info("재고 차감 요청 이벤트 파싱 완료 - orderId: {}, popupId: {}, items: {}",
                    event.getOrderId(), event.getPopupId(), event.getDeductionItems().size());

            // 각 항목별로 재고 차감 처리
            boolean allSuccess = true;
            String failureReason = null;

            for (StockDeductionRequestedEventDto.StockDeductionItem item : event.getDeductionItems()) {
                try {
                    log.info("굿즈 재고 차감 처리 - goodsVariantId: {}, quantity: {}",
                            item.getGoodsVariantId(), item.getQuantity());

                    // 실제 재고 차감 (기존 비즈니스 로직 재사용)
                    goodsService.completeReservationGoods(
                            event.getPopupId(),
                            item.getGoodsVariantId(),
                            item.getQuantity()
                    );

                    log.info("굿즈 재고 차감 성공 - goodsVariantId: {}", item.getGoodsVariantId());

                } catch (Exception e) {
                    log.error("굿즈 재고 차감 실패 - goodsVariantId: {}, error: {}",
                            item.getGoodsVariantId(), e.getMessage(), e);
                    allSuccess = false;
                    failureReason = "재고 차감 실패: " + e.getMessage();
                    break; // 하나라도 실패하면 전체 실패
                }
            }

            // 결과에 따라 성공/실패 이벤트 발행
            if (allSuccess) {
                publishStockDeductionSuccessEvent(event);
            } else {
                publishStockDeductionFailedEvent(event, failureReason);
            }

        } catch (Exception e) {
            log.error("재고 차감 요청 이벤트 처리 중 오류 - eventJson: {}, error: {}",
                    eventJson, e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 성공 이벤트 발행
     */
    private void publishStockDeductionSuccessEvent(StockDeductionRequestedEventDto originalEvent) {
        try {
            log.info("재고 차감 성공 이벤트 발행 시작 - orderId: {}", originalEvent.getOrderId());

            StockDeductionSuccessEventDto successEvent = StockDeductionSuccessEventDto.create(
                    originalEvent.getOrderId(),
                    originalEvent.getOrderNo(),
                    originalEvent.getPopupId(),
                    "재고 차감 완료: " + originalEvent.getDeductionItems().size() + "개 항목"
            );

            String eventJson = objectMapper.writeValueAsString(successEvent);
            redisTemplate.convertAndSend(STOCK_DEDUCTION_SUCCESS_TOPIC, eventJson);

            log.info("재고 차감 성공 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    originalEvent.getOrderId(), successEvent.getEventId());

        } catch (Exception e) {
            log.error("재고 차감 성공 이벤트 발행 실패 - orderId: {}, error: {}",
                    originalEvent.getOrderId(), e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 발행
     */
    private void publishStockDeductionFailedEvent(StockDeductionRequestedEventDto originalEvent, String reason) {
        try {
            log.info("재고 차감 실패 이벤트 발행 시작 - orderId: {}, reason: {}",
                    originalEvent.getOrderId(), reason);

            StockDeductionFailedEventDto failedEvent = StockDeductionFailedEventDto.forSystemError(
                    originalEvent.getOrderId(),
                    originalEvent.getOrderNo(),
                    reason
            );

            String eventJson = objectMapper.writeValueAsString(failedEvent);
            redisTemplate.convertAndSend(STOCK_DEDUCTION_FAILED_TOPIC, eventJson);

            log.info("재고 차감 실패 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    originalEvent.getOrderId(), failedEvent.getEventId());

        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 발행 실패 - orderId: {}, error: {}",
                    originalEvent.getOrderId(), e.getMessage(), e);
        }
    }

    // DTO classes for Redis serialization

    public static class StockDeductionRequestedEventDto {
        private String eventId;
        private UUID orderId;
        private String orderNo;
        private UUID popupId;
        private List<StockDeductionItem> deductionItems;
        private LocalDateTime requestedAt;

        public StockDeductionRequestedEventDto() {}

        // getters and setters
        public String getEventId() { return eventId; }
        public void setEventId(String eventId) { this.eventId = eventId; }

        public UUID getOrderId() { return orderId; }
        public void setOrderId(UUID orderId) { this.orderId = orderId; }

        public String getOrderNo() { return orderNo; }
        public void setOrderNo(String orderNo) { this.orderNo = orderNo; }

        public UUID getPopupId() { return popupId; }
        public void setPopupId(UUID popupId) { this.popupId = popupId; }

        public List<StockDeductionItem> getDeductionItems() { return deductionItems; }
        public void setDeductionItems(List<StockDeductionItem> deductionItems) { this.deductionItems = deductionItems; }

        public LocalDateTime getRequestedAt() { return requestedAt; }
        public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

        public static class StockDeductionItem {
            private UUID goodsVariantId;
            private Integer quantity;
            private String productName;
            private String variantName;

            public StockDeductionItem() {}

            // getters and setters
            public UUID getGoodsVariantId() { return goodsVariantId; }
            public void setGoodsVariantId(UUID goodsVariantId) { this.goodsVariantId = goodsVariantId; }

            public Integer getQuantity() { return quantity; }
            public void setQuantity(Integer quantity) { this.quantity = quantity; }

            public String getProductName() { return productName; }
            public void setProductName(String productName) { this.productName = productName; }

            public String getVariantName() { return variantName; }
            public void setVariantName(String variantName) { this.variantName = variantName; }
        }
    }

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
}