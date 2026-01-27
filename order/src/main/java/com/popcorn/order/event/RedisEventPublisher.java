package com.popcorn.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Redis Stream을 사용한 이벤트 퍼블리셔
 *
 * Redis Stream 방식으로 마이크로서비스 간 이벤트 통신 처리
 * - 메시지 지속성 보장 (Pub/Sub은 휘발성)
 * - Consumer Group을 통한 부하 분산
 * - 메시지 ACK 및 재처리 지원
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // Redis Stream 이름 상수
    private static final String ORDER_EVENTS_STREAM = "order-events";
    private static final String GOODS_EVENTS_STREAM = "goods-events";
    private static final String STOCK_EVENTS_STREAM = "stock-events";
    private static final String PRICE_EVENTS_STREAM = "price-events";

    /**
     * 주문 결제 완료 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishOrderPaidEvent(OrderPaidEvent event) {
        try {
            log.info("주문 결제 완료 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            Map<String, String> eventData = Map.of(
                "eventType", "order-paid",
                "orderId", event.getOrderId().toString(),
                "eventId", event.getEventId(),
                "orderNo", event.getOrderNo(),
                "userId", "", // userId 정보 별도 처리
                "totalAmount", event.getTotalAmount() != null ? event.getTotalAmount().toString() : "",
                "paidAt", event.getPaidAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(ORDER_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("주문 결제 완료 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("주문 결제 완료 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("주문 결제 완료 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 굿즈 재고 예약 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishGoodsReservationRequestedEvent(GoodsReservationRequestedEvent event) {
        try {
            log.info("굿즈 재고 예약 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            Map<String, String> eventData = Map.of(
                "eventType", "goods-reservation-requested",
                "eventId", event.getEventId(),
                "orderId", event.getOrderId().toString(),
                "orderNo", event.getOrderNo(),
                "popupId", event.getPopupId() != null ? event.getPopupId().toString() : "",
                "reservationItems", objectMapper.writeValueAsString(event.getReservationItems()),
                "requestedAt", event.getRequestedAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(GOODS_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("굿즈 재고 예약 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("굿즈 재고 예약 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("굿즈 재고 예약 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 재고 차감 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishStockDeductionRequestedEvent(StockDeductionRequestedEvent event) {
        try {
            log.info("재고 차감 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            Map<String, String> eventData = Map.of(
                "eventType", "stock-deduction-requested",
                "eventId", event.getEventId(),
                "orderId", event.getOrderId().toString(),
                "items", objectMapper.writeValueAsString(event.getDeductionItems() != null ? event.getDeductionItems() : "[]"),
                "requestedAt", event.getRequestedAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(STOCK_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("재고 차감 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("재고 차감 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("재고 차감 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 가격 조회 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishPriceLookupRequestedEvent(PriceLookupRequestedEvent event) {
        try {
            log.warn("🔥 [DEBUG] 가격 조회 요청 이벤트 Stream 발행 시작 - correlationId: {}, type: {}, goodsVariantId: {}",
                    event.getCorrelationId(), event.getRequestType(), event.getGoodsVariantId());

            // Redis Template 연결 상태 확인
            log.warn("🔥 [DEBUG] RedisTemplate 상태: {}", redisTemplate != null ? "NOT NULL" : "NULL");

            Map<String, String> eventData = Map.of(
                "eventType", "price-lookup-requested",
                "eventId", event.getEventId(),
                "correlationId", event.getCorrelationId(),
                "requestType", event.getRequestType(),
                "sessionId", event.getSessionId() != null ? event.getSessionId().toString() : "",
                "goodsVariantId", event.getGoodsVariantId() != null ? event.getGoodsVariantId().toString() : "",
                "requestedAt", event.getRequestedAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            log.warn("🔥 [DEBUG] 이벤트 데이터: {}", eventData);
            log.warn("🔥 [DEBUG] Stream 이름: {}", PRICE_EVENTS_STREAM);

            StringRecord record = StreamRecords.string(eventData).withStreamKey(PRICE_EVENTS_STREAM);
            log.warn("🔥 [DEBUG] StringRecord 생성 완료");

            String recordId = redisTemplate.opsForStream().add(record).getValue();
            log.warn("🔥 [DEBUG] Redis Stream 발행 완료 - recordId: {}", recordId);

            log.info("✅ 가격 조회 요청 이벤트 Stream 발행 완료 - correlationId: {}, type: {}, recordId: {}",
                    event.getCorrelationId(), event.getRequestType(), recordId);

        } catch (Exception e) {
            log.error("❌ 가격 조회 요청 이벤트 Stream 발행 실패 - correlationId: {}, error: {}",
                    event.getCorrelationId(), e.getMessage(), e);
            e.printStackTrace(); // 스택 트레이스도 출력
            throw new RuntimeException("가격 조회 요청 이벤트 Stream 발행 실패", e);
        }
    }

}
