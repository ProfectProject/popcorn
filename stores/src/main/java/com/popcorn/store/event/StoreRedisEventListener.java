package com.popcorn.store.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.popcorn.store.domain.goods.entity.ReservationType;
import com.popcorn.store.domain.goods.entity.GoodsVariant;
import com.popcorn.store.domain.goods.service.GoodsService;
import com.popcorn.store.domain.goods.service.GoodsOrderReservationService;
import com.popcorn.store.domain.goods.repository.GoodsVariantRepository;
import com.popcorn.store.domain.popup.entity.PopupSchedule;
import com.popcorn.store.domain.popup.repository.owner.jpa.JpaOwnerPopupScheduleRepository;
import com.popcorn.store.event.payment.InventoryConfirmationRequestedEvent;
import com.popcorn.store.event.order.OrderPaidEvent;

/**
 * Store 서비스 범용 이벤트 리스너
 * - Redis 이벤트 구독
 * - Spring Application 이벤트 구독
 * - 모든 이벤트를 로그로 기록
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreRedisEventListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final GoodsOrderReservationService reservationService;
    private final GoodsService goodsService;
    private final StoreRedisEventPublisher storeRedisEventPublisher;
    private final GoodsVariantRepository goodsVariantRepository;
    private final JpaOwnerPopupScheduleRepository popupScheduleRepository;

    /**
     * Redis Pub/Sub 이벤트 수신
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String channel = new String(message.getChannel());
            String body = new String(message.getBody());

            log.info("🔔 [STORES] Redis 이벤트 수신 - channel: {}, body: {}", channel, body);

            // 채널별 이벤트 처리
            handleRedisEvent(channel, body);

        } catch (Exception e) {
            log.error("🚨 [STORES] Redis 이벤트 처리 실패 - message: {}, error: {}",
                    new String(message.getBody()), e.getMessage(), e);
        }
    }

    /**
     * Redis 이벤트 처리
     */
    private void handleRedisEvent(String channel, String body) {
        try {
            switch (channel) {
                case "events:order-created":
                    log.info("📦 [STORES] 주문 생성 이벤트 수신 - {}", body);
                    // 재고 예약 로직 처리 시작
                    break;
                case "events:order-paid":
                    log.info("💳 [STORES] 주문 결제 완료 이벤트 수신 - {}", body);
                    publishOrderPaidEvent(body);
                    break;
                case "events:payment-approved":
                    log.info("✅ [STORES] 결제 승인 이벤트 수신 - {}", body);
                    // 재고 확정 처리
                    break;
                case "events:payment-failed":
                    log.warn("❌ [STORES] 결제 실패 이벤트 수신 - {}", body);
                    // 재고 복구 처리
                    break;
                case "events:inventory-confirmation-requested":
                    log.info("📦 [STORES] 재고 확정 요청 이벤트 수신 - {}", body);
                    publishInventoryConfirmationEvent(body);
                    break;
                case "events:inventory-restore-requested":
                    log.info("🔄 [STORES] 재고 복구 요청 이벤트 수신 - {}", body);
                    publishInventoryConfirmationEvent(body);
                    break;
                case "events:goods-reservation-requested":
                    log.info("📋 [STORES] 굿즈 재고 예약 요청 이벤트 수신 - {}", body);
                    publishGoodsReservationRequestedEvent(body);
                    break;
                case "events:price-lookup-requested":
                    log.info("💰 [STORES] 가격 조회 요청 이벤트 수신 - {}", body);
                    publishPriceLookupResponseEvent(body);
                    break;
                default:
                    log.info("🔔 [STORES] 기타 이벤트 수신 - channel: {}, body: {}", channel, body);
                    break;
            }
        } catch (Exception e) {
            log.error("🚨 [STORES] 이벤트 처리 실패 - channel: {}, error: {}", channel, e.getMessage(), e);
        }
    }

    private void publishInventoryConfirmationEvent(String body) {
        try {
            String resolvedBody = body;
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(body);
                if (node != null && node.isTextual()) {
                    resolvedBody = node.asText();
                }
            } catch (Exception ignored) {
            }

            InventoryConfirmationPayload payload =
                    objectMapper.readValue(resolvedBody, InventoryConfirmationPayload.class);
            publishInventoryConfirmationEventWithRetry(payload, 0);
        } catch (Exception e) {
            log.error("🚨 [STORES] 재고 처리 이벤트 변환 실패 - body: {}, error: {}",
                    body, e.getMessage(), e);
        }
    }

    private void publishInventoryConfirmationEventWithRetry(InventoryConfirmationPayload payload, int attempt) {
        boolean hasGoodsReservation = !reservationService
                .findByOrderIdAndType(payload.orderId, ReservationType.GOODS)
                .isEmpty();
        boolean hasScheduleReservation = !reservationService
                .findByOrderIdAndType(payload.orderId, ReservationType.SCHEDULE)
                .isEmpty();

        if (hasGoodsReservation || hasScheduleReservation) {
            InventoryConfirmationRequestedEvent event =
                    InventoryConfirmationRequestedEvent.fromPayload(
                            payload.paymentId,
                            payload.orderId,
                            payload.actionType,
                            payload.reason,
                            payload.requestedAt,
                            payload.occurredAt,
                            payload.eventId
                    );
            eventPublisher.publishEvent(event);
            log.info("📨 [STORES] 재고 처리 이벤트 발행 - orderId: {}, action: {}",
                    payload.orderId, payload.actionType);
            return;
        }

        if (attempt >= 5) {
            log.warn("⏳ 예약 정보 없음 - 재고 처리 이벤트 보류 종료 - orderId: {}",
                    payload.orderId);
            return;
        }

        int nextAttempt = attempt + 1;
        long delayMs = 200L * nextAttempt;
        log.info("⏳ 예약 정보 없음 - 재시도 예정 ({}회) - orderId: {}", nextAttempt, payload.orderId);
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            publishInventoryConfirmationEventWithRetry(payload, nextAttempt);
        });
    }

    private void publishOrderPaidEvent(String body) {
        try {
            String resolvedBody = body;
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(body);
                if (node != null && node.isTextual()) {
                    resolvedBody = node.asText();
                }
            } catch (Exception ignored) {
            }

            OrderPaidEvent event = objectMapper.readValue(resolvedBody, OrderPaidEvent.class);
            eventPublisher.publishEvent(event);
            log.info("📨 [STORES] 주문 결제 완료 이벤트 발행 - orderId: {}",
                    event.getOrderId());
        } catch (Exception e) {
            log.error("🚨 [STORES] 주문 결제 완료 이벤트 변환 실패 - body: {}, error: {}",
                    body, e.getMessage(), e);
        }
    }

    private void publishGoodsReservationRequestedEvent(String body) {
        try {
            String resolvedBody = body;
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(body);
                if (node != null && node.isTextual()) {
                    resolvedBody = node.asText();
                }
            } catch (Exception ignored) {
            }

            GoodsReservationRequestedPayload payload =
                    objectMapper.readValue(resolvedBody, GoodsReservationRequestedPayload.class);

            if (payload.reservationItems == null || payload.reservationItems.isEmpty()) {
                log.warn("📋 [STORES] 굿즈 재고 예약 요청 항목이 없음 - orderId: {}", payload.orderId);
                return;
            }

            for (GoodsReservationItem item : payload.reservationItems) {
                if (item.goodsVariantId == null || item.quantity == null) {
                    log.warn("📋 [STORES] 굿즈 재고 예약 요청 항목 누락 - orderId: {}, item: {}",
                            payload.orderId, item);
                    continue;
                }

                java.util.UUID popupId = payload.popupId;
                try {
                    if (popupId == null) {
                        popupId = goodsService.resolvePopupId(item.goodsVariantId);
                    }

                    goodsService.reservationGoods(popupId, item.goodsVariantId, item.quantity);
                    reservationService.createGoodsReservation(
                            payload.orderId,
                            payload.orderNo,
                            popupId,
                            item.goodsVariantId,
                            item.quantity
                    );

                    log.info("✅ [STORES] 굿즈 재고 예약 완료 - orderId: {}, goodsVariantId: {}, qty: {}",
                            payload.orderId, item.goodsVariantId, item.quantity);

                    storeRedisEventPublisher.publishGoodsReservedEvent(
                            payload.orderId,
                            popupId,
                            item.goodsVariantId,
                            item.quantity
                    );

                } catch (Exception e) {
                    log.error("❌ [STORES] 굿즈 재고 예약 실패 - orderId: {}, goodsVariantId: {}, qty: {}, error: {}",
                            payload.orderId, item.goodsVariantId, item.quantity, e.getMessage(), e);

                    storeRedisEventPublisher.publishGoodsReservationFailedEvent(
                            payload.orderId,
                            popupId,
                            item.goodsVariantId,
                            item.quantity,
                            0,
                            e.getMessage()
                    );
                }
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] 굿즈 재고 예약 요청 이벤트 변환 실패 - body: {}, error: {}",
                    body, e.getMessage(), e);
        }
    }

    private void publishPriceLookupResponseEvent(String body) {
        try {
            String resolvedBody = body;
            try {
                com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(body);
                if (node != null && node.isTextual()) {
                    resolvedBody = node.asText();
                }
            } catch (Exception ignored) {
            }

            PriceLookupRequestPayload payload =
                    objectMapper.readValue(resolvedBody, PriceLookupRequestPayload.class);

            if (payload.correlationId == null || payload.requestType == null) {
                log.warn("💰 [STORES] 가격 조회 요청 누락 - payload: {}", payload);
                return;
            }

            if ("SESSION".equals(payload.requestType)) {
                handleSessionPriceLookup(payload);
            } else if ("GOODS".equals(payload.requestType)) {
                handleGoodsPriceLookup(payload);
            } else {
                log.warn("💰 [STORES] 지원하지 않는 가격 조회 타입 - type: {}", payload.requestType);
                publishPriceLookupFailure(payload, "지원하지 않는 가격 조회 타입");
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] 가격 조회 요청 이벤트 변환 실패 - body: {}, error: {}",
                    body, e.getMessage(), e);
        }
    }

    private void handleSessionPriceLookup(PriceLookupRequestPayload payload) {
        if (payload.sessionId == null) {
            publishPriceLookupFailure(payload, "sessionId가 없습니다.");
            return;
        }

        PopupSchedule schedule = popupScheduleRepository.findById(payload.sessionId)
                .filter(value -> value.getDeletedAt() == null)
                .orElse(null);

        if (schedule == null || schedule.getPrice() == null) {
            publishPriceLookupFailure(payload, "세션 정보를 찾을 수 없습니다.");
            return;
        }

        StoreRedisEventPublisher.PriceLookupResponseEventDto response =
                StoreRedisEventPublisher.PriceLookupResponseEventDto.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .correlationId(payload.correlationId)
                        .requestType(payload.requestType)
                        .sessionId(payload.sessionId)
                        .price(schedule.getPrice())
                        .success(true)
                        .message("OK")
                        .respondedAt(java.time.LocalDateTime.now())
                        .eventTime(java.time.LocalDateTime.now())
                        .build();

        storeRedisEventPublisher.publishPriceLookupResponseEvent(response);
    }

    private void handleGoodsPriceLookup(PriceLookupRequestPayload payload) {
        if (payload.goodsVariantId == null) {
            publishPriceLookupFailure(payload, "goodsVariantId가 없습니다.");
            return;
        }

        GoodsVariant variant = goodsVariantRepository.findById(payload.goodsVariantId)
                .filter(value -> value.getDeletedAt() == null)
                .orElse(null);

        if (variant == null) {
            publishPriceLookupFailure(payload, "굿즈 정보를 찾을 수 없습니다.");
            return;
        }

        StoreRedisEventPublisher.PriceLookupResponseEventDto response =
                StoreRedisEventPublisher.PriceLookupResponseEventDto.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .correlationId(payload.correlationId)
                        .requestType(payload.requestType)
                        .goodsVariantId(payload.goodsVariantId)
                        .price(variant.getGoodsPrice())
                        .stockQuantity(variant.getStock())
                        .success(true)
                        .message("OK")
                        .respondedAt(java.time.LocalDateTime.now())
                        .eventTime(java.time.LocalDateTime.now())
                        .build();

        storeRedisEventPublisher.publishPriceLookupResponseEvent(response);
    }

    private void publishPriceLookupFailure(PriceLookupRequestPayload payload, String reason) {
        StoreRedisEventPublisher.PriceLookupResponseEventDto response =
                StoreRedisEventPublisher.PriceLookupResponseEventDto.builder()
                        .eventId(java.util.UUID.randomUUID().toString())
                        .correlationId(payload.correlationId)
                        .requestType(payload.requestType)
                        .sessionId(payload.sessionId)
                        .goodsVariantId(payload.goodsVariantId)
                        .success(false)
                        .message(reason)
                        .respondedAt(java.time.LocalDateTime.now())
                        .eventTime(java.time.LocalDateTime.now())
                        .build();

        storeRedisEventPublisher.publishPriceLookupResponseEvent(response);
    }

    private static class PriceLookupRequestPayload {
        public String eventId;
        public String correlationId;
        public String requestType;
        public java.util.UUID sessionId;
        public java.util.UUID goodsVariantId;
        public java.time.LocalDateTime requestedAt;
        public java.time.LocalDateTime eventTime;
    }

    /**
     * Spring Application 이벤트 수신 (범용)
     */
    @EventListener
    public void handleApplicationEvent(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();

            // Store 관련 이벤트는 더 자세히 로깅
            if (eventType.contains("Store") || eventType.contains("Popup") ||
                eventType.contains("Goods") || eventType.contains("Stock") ||
                eventType.contains("Inventory")) {
                log.info("🌟 [STORES] Store 관련 이벤트 수신 - type: {}, event: {}", eventType, event.toString());
            } else {
                log.debug("🔔 [STORES] Application 이벤트 수신 - type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] Application 이벤트 처리 실패 - event: {}, error: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    private static class InventoryConfirmationPayload {
        public java.util.UUID paymentId;
        public java.util.UUID orderId;
        public String actionType;
        public String reason;
        public String requestedAt;
        public String occurredAt;
        public String eventId;
    }

    private static class GoodsReservationRequestedPayload {
        public String eventId;
        public java.util.UUID orderId;
        public String orderNo;
        public java.util.UUID popupId;
        public java.util.List<GoodsReservationItem> reservationItems;
        public String requestedAt;
    }

    private static class GoodsReservationItem {
        public java.util.UUID goodsVariantId;
        public Integer quantity;
    }
}
