package com.popcorn.store.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.store.event.order.StockDeductionFailedEvent;
import com.popcorn.store.event.order.StockDeductionSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stores 서비스 Redis Stream 이벤트 발행자
 * 재고 차감 결과 및 가격 조회 응답을 다른 마이크로서비스들에게 전파
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreRedisEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Stream 이름 상수
    private static final String STOCK_EVENTS_STREAM = "stock-events";
    private static final String RESPONSE_EVENTS_STREAM = "response-events";
    private static final String INVENTORY_EVENTS_STREAM = "inventory-events";
    private static final String GOODS_EVENTS_STREAM = "goods-events";

    /**
     * 재고 차감 성공 이벤트 발행
     */
    public void publishStockDeductionSuccessEvent(StockDeductionSuccessEvent event) {
        try {
            log.info("🚀 [STORES] 재고 차감 성공 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            Map<String, Object> eventData = Map.of(
                "eventType", "stock-deduction-success",
                "eventId", event.getEventId(),
                "orderId", event.getOrderId().toString(),
                "orderNo", event.getOrderNo(),
                "popupId", event.getPopupId().toString(),
                "stockDetails", event.getStockDetails(),
                "succeededAt", event.getSucceededAt().toString(),
                "eventTime", java.time.LocalDateTime.now().toString()
            );

            // Map<String, Object>를 Map<String, String>으로 변환
            Map<String, String> stringEventData = eventData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
            stringEventData.put("eventTime", LocalDateTime.now().toString());

            StringRecord record = StreamRecords.string(stringEventData).withStreamKey(STOCK_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("✅ [STORES] 재고 차감 성공 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("❌ [STORES] 재고 차감 성공 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 발행
     */
    public void publishStockDeductionFailedEvent(StockDeductionFailedEvent event) {
        try {
            log.info("🚀 [STORES] 재고 차감 실패 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            Map<String, Object> eventData = Map.of(
                "eventType", "stock-deduction-failed",
                "eventId", event.getEventId(),
                "orderId", event.getOrderId().toString(),
                "orderNo", event.getOrderNo(),
                "reason", event.getReason(),
                "failureCode", event.getFailureCode() != null ? event.getFailureCode() : "",
                "failedAt", event.getFailedAt().toString(),
                "eventTime", java.time.LocalDateTime.now().toString()
            );

            // Map<String, Object>를 Map<String, String>으로 변환
            Map<String, String> stringEventData = eventData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
            stringEventData.put("eventTime", LocalDateTime.now().toString());

            StringRecord record = StreamRecords.string(stringEventData).withStreamKey(STOCK_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("⚠️ [STORES] 재고 차감 실패 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}, reason: {}",
                    event.getOrderId(), event.getEventId(), event.getReason());

        } catch (Exception e) {
            log.error("❌ [STORES] 재고 차감 실패 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 재고 업데이트 이벤트 발행 (일반적인 재고 변경)
     */
    public void publishInventoryUpdatedEvent(java.util.UUID popupId, java.util.UUID goodsVariantId,
                                           int oldQuantity, int newQuantity, String reason) {
        try {
            log.info("🚀 [STORES] 재고 업데이트 이벤트 Stream 발행 - popupId: {}, goodsVariantId: {}, {}→{}, reason: {}",
                    popupId, goodsVariantId, oldQuantity, newQuantity, reason);

            String eventId = java.util.UUID.randomUUID().toString();
            Map<String, Object> eventData = Map.of(
                "eventType", "inventory-updated",
                "eventId", eventId,
                "popupId", popupId.toString(),
                "goodsVariantId", goodsVariantId.toString(),
                "oldQuantity", Integer.toString(oldQuantity),
                "newQuantity", Integer.toString(newQuantity),
                "reason", reason,
                "updatedAt", java.time.LocalDateTime.now().toString(),
                "eventTime", java.time.LocalDateTime.now().toString()
            );

            // Map<String, Object>를 Map<String, String>으로 변환
            Map<String, String> stringEventData = eventData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
            stringEventData.put("eventTime", LocalDateTime.now().toString());

            StringRecord record = StreamRecords.string(stringEventData).withStreamKey(INVENTORY_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("✅ [STORES] 재고 업데이트 이벤트 Stream 발행 완료 - eventId: {}", eventId);

        } catch (Exception e) {
            log.error("❌ [STORES] 재고 업데이트 이벤트 Stream 발행 실패 - popupId: {}, error: {}", popupId, e.getMessage(), e);
        }
    }

    /**
     * 굿즈 예약 성공 이벤트 발행
     */
    public void publishGoodsReservedEvent(java.util.UUID orderId, java.util.UUID popupId,
                                        java.util.UUID goodsVariantId, int quantity) {
        try {
            log.info("🚀 [STORES] 굿즈 예약 성공 이벤트 Stream 발행 - orderId: {}, goodsVariantId: {}, quantity: {}",
                    orderId, goodsVariantId, quantity);

            String eventId = java.util.UUID.randomUUID().toString();
            Map<String, Object> eventData = Map.of(
                "eventType", "goods-reserved",
                "eventId", eventId,
                "orderId", orderId.toString(),
                "popupId", popupId.toString(),
                "goodsVariantId", goodsVariantId.toString(),
                "quantity", Integer.toString(quantity),
                "reservedAt", java.time.LocalDateTime.now().toString(),
                "eventTime", java.time.LocalDateTime.now().toString()
            );

            // Map<String, Object>를 Map<String, String>으로 변환
            Map<String, String> stringEventData = eventData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
            stringEventData.put("eventTime", LocalDateTime.now().toString());

            StringRecord record = StreamRecords.string(stringEventData).withStreamKey(GOODS_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("✅ [STORES] 굿즈 예약 성공 이벤트 Stream 발행 완료 - eventId: {}", eventId);

        } catch (Exception e) {
            log.error("❌ [STORES] 굿즈 예약 성공 이벤트 Stream 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 굿즈 예약 실패 이벤트 발행
     */
    public void publishGoodsReservationFailedEvent(java.util.UUID orderId, java.util.UUID popupId,
                                                  java.util.UUID goodsVariantId, int requestedQuantity,
                                                  int availableQuantity, String reason) {
        try {
            log.info("🚀 [STORES] 굿즈 예약 실패 이벤트 Stream 발행 - orderId: {}, goodsVariantId: {}, reason: {}",
                    orderId, goodsVariantId, reason);

            String eventId = java.util.UUID.randomUUID().toString();
            Map<String, Object> eventData = Map.of(
                "eventType", "goods-reservation-failed",
                "eventId", eventId,
                "orderId", orderId.toString(),
                "popupId", popupId != null ? popupId.toString() : "",
                "goodsVariantId", goodsVariantId.toString(),
                "requestedQuantity", Integer.toString(requestedQuantity),
                "availableQuantity", Integer.toString(availableQuantity),
                "reason", reason,
                "failedAt", java.time.LocalDateTime.now().toString(),
                "eventTime", java.time.LocalDateTime.now().toString()
            );

            // Map<String, Object>를 Map<String, String>으로 변환
            Map<String, String> stringEventData = eventData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
            stringEventData.put("eventTime", LocalDateTime.now().toString());

            StringRecord record = StreamRecords.string(stringEventData).withStreamKey(GOODS_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("⚠️ [STORES] 굿즈 예약 실패 이벤트 Stream 발행 완료 - eventId: {}, reason: {}", eventId, reason);

        } catch (Exception e) {
            log.error("❌ [STORES] 굿즈 예약 실패 이벤트 Stream 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 가격 조회 응답 이벤트 발행
     */
    public void publishPriceLookupResponseEvent(PriceLookupResponseEventDto event) {
        try {
            log.info("🚀 [STORES] 가격 조회 응답 이벤트 Stream 발행 - correlationId: {}, type: {}",
                    event.getCorrelationId(), event.getRequestType());

            Map<String, Object> eventData = new java.util.HashMap<>();
            eventData.put("eventType", "price-lookup-response");
            eventData.put("eventId", event.getEventId());
            eventData.put("correlationId", event.getCorrelationId());
            eventData.put("requestType", event.getRequestType());
            eventData.put("sessionId", event.getSessionId() != null ? event.getSessionId().toString() : "");
            eventData.put("goodsVariantId", event.getGoodsVariantId() != null ? event.getGoodsVariantId().toString() : "");
            eventData.put("price", event.getPrice() != null ? event.getPrice().toString() : "");
            eventData.put("stockQuantity", event.getStockQuantity() != null ? event.getStockQuantity().toString() : "");
            eventData.put("success", Boolean.toString(event.isSuccess()));
            eventData.put("message", event.getMessage() != null ? event.getMessage() : "");
            eventData.put("respondedAt", event.getRespondedAt().toString());
            eventData.put("eventTime", event.getEventTime().toString());

            // Map<String, Object>를 Map<String, String>으로 변환
            Map<String, String> stringEventData = eventData.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue() != null ? e.getValue().toString() : ""
                ));
            stringEventData.put("eventTime", LocalDateTime.now().toString());

            StringRecord record = StreamRecords.string(stringEventData).withStreamKey(RESPONSE_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("✅ [STORES] 가격 조회 응답 이벤트 Stream 발행 완료 - correlationId: {}, success: {}",
                    event.getCorrelationId(), event.isSuccess());

        } catch (Exception e) {
            log.error("❌ [STORES] 가격 조회 응답 이벤트 Stream 발행 실패 - correlationId: {}, error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
        }
    }

    // DTO classes for Redis Stream serialization (only for complex responses)

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PriceLookupResponseEventDto {
        private String eventId;
        private String correlationId;
        private String requestType;
        private java.util.UUID sessionId;
        private java.util.UUID goodsVariantId;
        private Integer price;
        private Integer stockQuantity;
        private boolean success;
        private String message;
        private java.time.LocalDateTime respondedAt;
        private java.time.LocalDateTime eventTime;
    }
}
