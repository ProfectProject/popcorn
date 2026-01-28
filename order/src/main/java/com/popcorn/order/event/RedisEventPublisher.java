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
    private static final String ORDER_EVENTS_STREAM = "order-events";    // 주문 생성, 결제 완료 등
    private static final String SCHEDULE_EVENTS_STREAM = "schedule-events";  // 스케줄 예약
    private static final String GOODS_EVENTS_STREAM = "goods-events";        // 굿즈 예약
    private static final String MIXED_EVENTS_STREAM = "mixed-events";        // 복합형 (스케줄+굿즈)
    private static final String STOCK_EVENTS_STREAM = "stock-events";        // 재고 차감
    private static final String PRICE_EVENTS_STREAM = "price-events";        // 가격 조회
    private static final String PAYMENT_EVENTS_STREAM = "payment-events";    // 결제
    private static final String STORE_LOOKUP_STREAM = "store-lookup-requests";

    /**
     * 주문 결제 완료 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishOrderPaidEvent(OrderPaidEvent event) {
        try {
            log.info("주문 결제 완료 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            java.util.Map<String, String> eventData = new java.util.HashMap<>();
            eventData.put("eventType", "order-paid");
            eventData.put("orderId", event.getOrderId().toString());
            eventData.put("eventId", event.getEventId());
            eventData.put("orderNo", event.getOrderNo());
            eventData.put("userId", event.getCustomerId() != null ? event.getCustomerId().toString() : "");
            eventData.put("popupId", event.getPopupId() != null ? event.getPopupId().toString() : "");
            eventData.put("orderType", event.getOrderType() != null ? event.getOrderType() : "");
            eventData.put("totalAmount", event.getTotalAmount() != null ? event.getTotalAmount().toString() : "");
            eventData.put("orderItems", objectMapper.writeValueAsString(
                    event.getOrderItems() != null ? event.getOrderItems() : java.util.List.of()
            ));
            eventData.put("paidAt", event.getPaidAt().toString());
            eventData.put("eventTime", LocalDateTime.now().toString());

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
                "orderNo", event.getOrderNo() != null ? event.getOrderNo() : "",
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
     * 굿즈 예약 취소 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishGoodsReservationCancelRequestedEvent(String eventId,
                                                            java.util.UUID orderId,
                                                            java.util.UUID popupId,
                                                            java.util.UUID goodsId,
                                                            Integer quantity) {
        try {
            log.info("굿즈 예약 취소 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    orderId, eventId);

            Map<String, String> eventData = Map.of(
                "eventType", "goods-reservation-cancel-requested",
                "eventId", eventId,
                "orderId", orderId.toString(),
                "popupId", popupId != null ? popupId.toString() : "",
                "goodsId", goodsId != null ? goodsId.toString() : "",
                "quantity", quantity != null ? quantity.toString() : "",
                "requestedAt", LocalDateTime.now().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(GOODS_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("굿즈 예약 취소 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    orderId, eventId);

        } catch (Exception e) {
            log.error("굿즈 예약 취소 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    orderId, eventId, e.getMessage(), e);
            throw new RuntimeException("굿즈 예약 취소 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 결제 생성 요청 이벤트 발행 (Payment 서비스에서 수신)
     */
    public void publishPaymentCreateRequestedEvent(String eventId,
                                                   java.util.UUID orderId,
                                                   String orderNo,
                                                   Integer amount,
                                                   String paymentMethod,
                                                   Long customerId,
                                                   String paymentKey) {
        try {
            log.info("결제 생성 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    orderId, eventId);

            Map<String, String> eventData = Map.of(
                "eventType", "payment-create-requested",
                "eventId", eventId,
                "orderId", orderId.toString(),
                "orderNo", orderNo != null ? orderNo : "",
                "amount", amount != null ? amount.toString() : "",
                "paymentMethod", paymentMethod != null ? paymentMethod : "",
                "customerId", customerId != null ? customerId.toString() : "",
                "paymentKey", paymentKey != null ? paymentKey : "",
                "requestedAt", LocalDateTime.now().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(PAYMENT_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("결제 생성 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    orderId, eventId);

        } catch (Exception e) {
            log.error("결제 생성 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    orderId, eventId, e.getMessage(), e);
            throw new RuntimeException("결제 생성 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 결제 취소 요청 이벤트 발행 (Payment 서비스에서 수신)
     */
    public void publishPaymentCancelRequestedEvent(String eventId,
                                                   java.util.UUID orderId,
                                                   String orderNo,
                                                   String paymentId,
                                                   String reason,
                                                   Long customerId) {
        try {
            log.info("결제 취소 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    orderId, eventId);

            Map<String, String> eventData = Map.of(
                "eventType", "payment-cancel-requested",
                "eventId", eventId,
                "orderId", orderId.toString(),
                "orderNo", orderNo != null ? orderNo : "",
                "paymentId", paymentId != null ? paymentId : "",
                "reason", reason != null ? reason : "",
                "customerId", customerId != null ? customerId.toString() : "",
                "requestedAt", LocalDateTime.now().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(PAYMENT_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("결제 취소 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    orderId, eventId);

        } catch (Exception e) {
            log.error("결제 취소 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    orderId, eventId, e.getMessage(), e);
            throw new RuntimeException("결제 취소 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 팝업 정보 조회 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishPopupInfoLookupRequestedEvent(String eventId,
                                                     String correlationId,
                                                     java.util.UUID popupId) {
        try {
            log.info("팝업 정보 조회 요청 이벤트 Stream 발행 시작 - popupId: {}, correlationId: {}",
                    popupId, correlationId);

            Map<String, String> eventData = Map.of(
                "eventType", "popup-info-lookup-requested",
                "eventId", eventId,
                "correlationId", correlationId,
                "popupId", popupId.toString(),
                "requestedAt", LocalDateTime.now().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(STORE_LOOKUP_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("팝업 정보 조회 요청 이벤트 Stream 발행 완료 - popupId: {}, correlationId: {}",
                    popupId, correlationId);

        } catch (Exception e) {
            log.error("팝업 정보 조회 요청 이벤트 Stream 발행 실패 - popupId: {}, error: {}",
                    popupId, e.getMessage(), e);
            throw new RuntimeException("팝업 정보 조회 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 가격 조회 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishPriceLookupRequestedEvent(PriceLookupRequestedEvent event) {
        try {
            log.warn("🔥 [DEBUG] 가격 조회 요청 이벤트 Stream 발행 시작 - correlationId: {}, type: {}, goodsId: {}",
                    event.getCorrelationId(), event.getRequestType(), event.getGoodsId());

            // Redis Template 연결 상태 확인
            log.warn("🔥 [DEBUG] RedisTemplate 상태: {}", redisTemplate != null ? "NOT NULL" : "NULL");

            Map<String, String> eventData = Map.of(
                "eventType", "price-lookup-requested",
                "eventId", event.getEventId(),
                "correlationId", event.getCorrelationId(),
                "requestType", event.getRequestType(),
                "sessionId", event.getSessionId() != null ? event.getSessionId().toString() : "",
                "goodsId", event.getGoodsId() != null ? event.getGoodsId().toString() : "",
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

    /**
     * User 주소 조회 요청 이벤트 발행
     */
    public void publishUserAddressLookupRequest(UserAddressLookupRequestedEvent event) {
        try {
            log.info("사용자 주소 조회 요청 이벤트 Stream 발행 시작 - userId: {}, correlationId: {}",
                    event.getUserId(), event.getCorrelationId());

            // 이벤트 데이터 맵 생성
            Map<String, String> eventData = Map.of(
                    "eventId", event.getEventId(),
                    "correlationId", event.getCorrelationId(),
                    "requestType", event.getRequestType(),
                    "userId", String.valueOf(event.getUserId()),
                    "requestedAt", event.getRequestedAt().toString(),
                    "eventType", "user-address-lookup-requested",
                    "eventTime", LocalDateTime.now().toString()
            );

            // StringRecord 생성
            StringRecord record = StreamRecords.string(eventData)
                    .withStreamKey("user-address-events");

            // Redis Stream에 발행
            String recordId = redisTemplate.opsForStream().add(record).getValue();

            log.info("✅ 사용자 주소 조회 요청 이벤트 Stream 발행 완료 - userId: {}, correlationId: {}, recordId: {}",
                    event.getUserId(), event.getCorrelationId(), recordId);

        } catch (Exception e) {
            log.error("❌ 사용자 주소 조회 요청 이벤트 Stream 발행 실패 - userId: {}, correlationId: {}, error: {}",
                    event.getUserId(), event.getCorrelationId(), e.getMessage(), e);
            throw new RuntimeException("사용자 주소 조회 요청 이벤트 Stream 발행 실패", e);
        }
    }

    // ================ 📅 스케줄 예약 관련 이벤트 발행 메소드들 ================

    /**
     * 스케줄 예약 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishScheduleReservationRequestedEvent(ScheduleReservationRequestedEvent event) {
        try {
            log.info("📅 [ORDER→STORE] 스케줄 예약 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            // 예약 항목들을 JSON으로 직렬화
            String reservationItemsJson = objectMapper.writeValueAsString(event.getReservationItems());

            Map<String, String> eventData = Map.of(
                "eventType", "schedule-reservation-requested",
                "eventId", event.getEventId(),
                "orderId", event.getOrderId().toString(),
                "orderNo", event.getOrderNo(),
                "popupId", event.getPopupId().toString(),
                "reservationItems", reservationItemsJson,
                "requestedAt", event.getRequestedAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(SCHEDULE_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("📅✅ [ORDER→STORE] 스케줄 예약 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("📅❌ [ORDER→STORE] 스케줄 예약 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("스케줄 예약 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 스케줄 예약 취소 요청 이벤트 발행 (Store 서비스에서 수신)
     */
    public void publishScheduleReservationCancelRequestedEvent(ScheduleReservationCancelRequestedEvent event) {
        try {
            log.info("📅 [ORDER→STORE] 스케줄 예약 취소 요청 이벤트 Stream 발행 시작 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

            // 취소 항목들을 JSON으로 직렬화
            String cancelItemsJson = objectMapper.writeValueAsString(event.getCancelItems());

            Map<String, String> eventData = Map.of(
                "eventType", "schedule-reservation-cancel-requested",
                "eventId", event.getEventId(),
                "orderId", event.getOrderId().toString(),
                "orderNo", event.getOrderNo(),
                "popupId", event.getPopupId().toString(),
                "reservationToken", event.getReservationToken() != null ? event.getReservationToken() : "",
                "cancelItems", cancelItemsJson,
                "cancelReason", event.getCancelReason(),
                "cancelRequestedAt", event.getCancelRequestedAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(SCHEDULE_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("📅✅ [ORDER→STORE] 스케줄 예약 취소 요청 이벤트 Stream 발행 완료 - orderId: {}, eventId: {}",
                    event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("📅❌ [ORDER→STORE] 스케줄 예약 취소 요청 이벤트 Stream 발행 실패 - orderId: {}, eventId: {}, error: {}",
                    event.getOrderId(), event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("스케줄 예약 취소 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 복합형 예약 요청 이벤트 발행 (스케줄 + 굿즈) - Store 서비스에서 hold_both.lua 사용
     */
    public void publishMixedReservationRequestedEvent(
            ScheduleReservationRequestedEvent scheduleEvent,
            GoodsReservationRequestedEvent goodsEvent) {
        try {
            log.info("🔗 [ORDER→STORE] 복합형 예약 요청 이벤트 Stream 발행 시작 - orderId: {}",
                    scheduleEvent.getOrderId());

            // 스케줄 예약 항목들을 JSON으로 직렬화
            String scheduleItemsJson = objectMapper.writeValueAsString(scheduleEvent.getReservationItems());

            // 굿즈 예약 항목들을 JSON으로 직렬화
            String goodsItemsJson = objectMapper.writeValueAsString(goodsEvent.getReservationItems());

            Map<String, String> eventData = Map.of(
                "eventType", "mixed-reservation-requested",
                "eventId", java.util.UUID.randomUUID().toString(),
                "orderId", scheduleEvent.getOrderId().toString(),
                "orderNo", scheduleEvent.getOrderNo(),
                "popupId", scheduleEvent.getPopupId() != null ? scheduleEvent.getPopupId().toString() : "",
                "scheduleItems", scheduleItemsJson,
                "goodsItems", goodsItemsJson,
                "requestedAt", scheduleEvent.getRequestedAt().toString(),
                "eventTime", LocalDateTime.now().toString()
            );

            StringRecord record = StreamRecords.string(eventData).withStreamKey(MIXED_EVENTS_STREAM);
            redisTemplate.opsForStream().add(record);

            log.info("🔗✅ [ORDER→STORE] 복합형 예약 요청 이벤트 Stream 발행 완료 - orderId: {}",
                    scheduleEvent.getOrderId());

        } catch (Exception e) {
            log.error("🔗❌ [ORDER→STORE] 복합형 예약 요청 이벤트 Stream 발행 실패 - orderId: {}, error: {}",
                    scheduleEvent.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("복합형 예약 요청 이벤트 Stream 발행 실패", e);
        }
    }

    /**
     * 스케줄 확정 요청 이벤트 Stream 발행 (결제 완료 후)
     * Store 서비스로 스케줄 예약 → 확정 변경 요청 전송
     */
    public void publishScheduleConfirmationRequestedEvent(
            String eventId,
            java.util.UUID orderId,
            String orderNo,
            java.util.UUID popupId,
            java.util.List<com.popcorn.order.service.OrderCommandService.ScheduleConfirmationItem> confirmationItems) {

        try {
            log.warn("🔥 [DEBUG] 스케줄 확정 요청 이벤트 Stream 발행 시작 - eventId: {}, orderId: {}",
                    eventId, orderId);

            // 확정 항목들을 JSON 형태로 직렬화
            String confirmationItemsJson = objectMapper.writeValueAsString(
                confirmationItems.stream()
                    .map(item -> java.util.Map.of(
                        "scheduleId", item.getScheduleId().toString(),
                        "quantity", item.getQuantity().toString(),
                        "sessionName", item.getSessionName() != null ? item.getSessionName() : "",
                        "sessionTime", item.getSessionTime() != null ? item.getSessionTime() : ""
                    ))
                    .toList()
            );

            // Stream 이벤트 데이터 구성
            java.util.Map<String, Object> eventData = new java.util.HashMap<>();
            eventData.put("eventType", "schedule-confirmation-requested");
            eventData.put("eventId", eventId);
            eventData.put("orderId", orderId.toString());
            eventData.put("orderNo", orderNo);
            eventData.put("popupId", popupId != null ? popupId.toString() : "");
            eventData.put("confirmationItems", confirmationItemsJson);
            eventData.put("requestedAt", java.time.LocalDateTime.now().toString());
            eventData.put("eventTime", java.time.LocalDateTime.now().toString());

            log.warn("🔥 [DEBUG] 스케줄 확정 이벤트 데이터: {}", eventData);

            // String 값으로 변환
            java.util.Map<String, String> stringEventData = eventData.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        java.util.Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                    ));

            // Redis Stream Record 생성 및 발행
            StringRecord record = StreamRecords.string(stringEventData)
                    .withStreamKey("schedule-events");

            log.warn("🔥 [DEBUG] StringRecord 생성 완료");

            String recordId = redisTemplate.opsForStream().add(record).getValue();

            log.warn("🔥 [DEBUG] Redis Stream 발행 완료 - recordId: {}", recordId);
            log.info("✅ 스케줄 확정 요청 이벤트 Stream 발행 완료 - eventId: {}, orderId: {}, recordId: {}",
                    eventId, orderId, recordId);

        } catch (Exception e) {
            log.error("❌ [ORDER→STORE] 스케줄 확정 요청 이벤트 Stream 발행 실패 - orderId: {}, error: {}",
                    orderId, e.getMessage(), e);
            throw new RuntimeException("스케줄 확정 요청 이벤트 Stream 발행 실패", e);
        }
    }

}
