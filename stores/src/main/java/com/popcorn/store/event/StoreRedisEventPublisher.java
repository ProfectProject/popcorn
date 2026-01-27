package com.popcorn.store.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.store.event.order.StockDeductionFailedEvent;
import com.popcorn.store.event.order.StockDeductionSuccessEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Stores 서비스 Redis 이벤트 발행자
 * 재고 차감 결과를 다른 마이크로서비스들에게 전파
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreRedisEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // 이벤트 토픽 상수
    private static final String STOCK_DEDUCTION_SUCCESS_TOPIC = "events:stock-deduction-success";
    private static final String STOCK_DEDUCTION_FAILED_TOPIC = "events:stock-deduction-failed";
    private static final String INVENTORY_UPDATED_TOPIC = "events:inventory-updated";
    private static final String GOODS_RESERVED_TOPIC = "events:goods-reserved";
    private static final String GOODS_RESERVATION_FAILED_TOPIC = "events:goods-reservation-failed";
    private static final String PRICE_LOOKUP_RESPONSE_TOPIC = "events:price-lookup-response";

    /**
     * 재고 차감 성공 이벤트 발행
     */
    public void publishStockDeductionSuccessEvent(StockDeductionSuccessEvent event) {
        try {
            log.info("🚀 [STORES] 재고 차감 성공 이벤트 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(STOCK_DEDUCTION_SUCCESS_TOPIC, eventJson);

            log.info("✅ [STORES] 재고 차감 성공 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("❌ [STORES] 재고 차감 성공 이벤트 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 발행
     */
    public void publishStockDeductionFailedEvent(StockDeductionFailedEvent event) {
        try {
            log.info("🚀 [STORES] 재고 차감 실패 이벤트 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(STOCK_DEDUCTION_FAILED_TOPIC, eventJson);

            log.info("⚠️ [STORES] 재고 차감 실패 이벤트 발행 완료 - orderId: {}, eventId: {}, reason: {}",
                    event.getOrderId(), event.getEventId(), event.getReason());

        } catch (Exception e) {
            log.error("❌ [STORES] 재고 차감 실패 이벤트 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * 재고 업데이트 이벤트 발행 (일반적인 재고 변경)
     */
    public void publishInventoryUpdatedEvent(java.util.UUID popupId, java.util.UUID goodsVariantId,
                                           int oldQuantity, int newQuantity, String reason) {
        try {
            log.info("🚀 [STORES] 재고 업데이트 이벤트 발행 - popupId: {}, goodsVariantId: {}, {}→{}, reason: {}",
                    popupId, goodsVariantId, oldQuantity, newQuantity, reason);

            InventoryUpdatedEventDto event = InventoryUpdatedEventDto.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .popupId(popupId)
                    .goodsVariantId(goodsVariantId)
                    .oldQuantity(oldQuantity)
                    .newQuantity(newQuantity)
                    .reason(reason)
                    .updatedAt(java.time.LocalDateTime.now())
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(INVENTORY_UPDATED_TOPIC, eventJson);

            log.info("✅ [STORES] 재고 업데이트 이벤트 발행 완료 - eventId: {}", event.getEventId());

        } catch (Exception e) {
            log.error("❌ [STORES] 재고 업데이트 이벤트 발행 실패 - popupId: {}, error: {}", popupId, e.getMessage(), e);
        }
    }

    /**
     * 굿즈 예약 성공 이벤트 발행
     */
    public void publishGoodsReservedEvent(java.util.UUID orderId, java.util.UUID popupId,
                                        java.util.UUID goodsVariantId, int quantity) {
        try {
            log.info("🚀 [STORES] 굿즈 예약 성공 이벤트 발행 - orderId: {}, goodsVariantId: {}, quantity: {}",
                    orderId, goodsVariantId, quantity);

            GoodsReservedEventDto event = GoodsReservedEventDto.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .orderId(orderId)
                    .popupId(popupId)
                    .goodsVariantId(goodsVariantId)
                    .quantity(quantity)
                    .reservedAt(java.time.LocalDateTime.now())
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(GOODS_RESERVED_TOPIC, eventJson);

            log.info("✅ [STORES] 굿즈 예약 성공 이벤트 발행 완료 - eventId: {}", event.getEventId());

        } catch (Exception e) {
            log.error("❌ [STORES] 굿즈 예약 성공 이벤트 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 굿즈 예약 실패 이벤트 발행
     */
    public void publishGoodsReservationFailedEvent(java.util.UUID orderId, java.util.UUID popupId,
                                                  java.util.UUID goodsVariantId, int requestedQuantity,
                                                  int availableQuantity, String reason) {
        try {
            log.info("🚀 [STORES] 굿즈 예약 실패 이벤트 발행 - orderId: {}, goodsVariantId: {}, reason: {}",
                    orderId, goodsVariantId, reason);

            GoodsReservationFailedEventDto event = GoodsReservationFailedEventDto.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .orderId(orderId)
                    .popupId(popupId)
                    .goodsVariantId(goodsVariantId)
                    .requestedQuantity(requestedQuantity)
                    .availableQuantity(availableQuantity)
                    .reason(reason)
                    .failedAt(java.time.LocalDateTime.now())
                    .build();

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(GOODS_RESERVATION_FAILED_TOPIC, eventJson);

            log.info("⚠️ [STORES] 굿즈 예약 실패 이벤트 발행 완료 - eventId: {}, reason: {}", event.getEventId(), reason);

        } catch (Exception e) {
            log.error("❌ [STORES] 굿즈 예약 실패 이벤트 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }
    }

    /**
     * 가격 조회 응답 이벤트 발행
     */
    public void publishPriceLookupResponseEvent(PriceLookupResponseEventDto event) {
        try {
            log.info("🚀 [STORES] 가격 조회 응답 이벤트 발행 - correlationId: {}, type: {}",
                    event.getCorrelationId(), event.getRequestType());

            String eventJson = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend(PRICE_LOOKUP_RESPONSE_TOPIC, eventJson);

            log.info("✅ [STORES] 가격 조회 응답 이벤트 발행 완료 - correlationId: {}",
                    event.getCorrelationId());

        } catch (Exception e) {
            log.error("❌ [STORES] 가격 조회 응답 이벤트 발행 실패 - correlationId: {}, error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
        }
    }

    // DTO classes for Redis serialization

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class InventoryUpdatedEventDto {
        private String eventId;
        private java.util.UUID popupId;
        private java.util.UUID goodsVariantId;
        private int oldQuantity;
        private int newQuantity;
        private String reason;
        private java.time.LocalDateTime updatedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GoodsReservedEventDto {
        private String eventId;
        private java.util.UUID orderId;
        private java.util.UUID popupId;
        private java.util.UUID goodsVariantId;
        private int quantity;
        private java.time.LocalDateTime reservedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GoodsReservationFailedEventDto {
        private String eventId;
        private java.util.UUID orderId;
        private java.util.UUID popupId;
        private java.util.UUID goodsVariantId;
        private int requestedQuantity;
        private int availableQuantity;
        private String reason;
        private java.time.LocalDateTime failedAt;
    }

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
