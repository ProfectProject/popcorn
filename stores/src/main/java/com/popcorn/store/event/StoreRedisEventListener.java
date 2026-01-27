package com.popcorn.store.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import com.popcorn.store.domain.goods.entity.GoodsOrderReservation;
import com.popcorn.store.domain.goods.entity.ReservationStatus;
import com.popcorn.store.domain.goods.entity.ReservationType;
import com.popcorn.store.domain.goods.service.GoodsService;
import com.popcorn.store.domain.goods.service.GoodsOrderReservationService;
import com.popcorn.store.event.payment.InventoryConfirmationRequestedEvent;
import com.popcorn.store.event.order.OrderPaidEvent;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService.GoodsHoldItem;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService.HoldResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService.GoodsHoldItem;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService.HoldResult;

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
    private final InventoryRedisHoldService inventoryHoldService;

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

    /**
     * Order 서비스에서 발행한 굿즈 재고 예약 요청을 처리하는 메서드
     * 1. JSON 파싱 후 ORDER/POPUP 정보를 추출
     * 2. 각 굿즈마다 availability 키를 계산하고 Redis Lua로 HOLD
     * 3. HOLD 성공 시 예약 행을 HELD 상태로 저장하고, 실패 시 RELEASE + 실패 이벤트
     * 4. Redis 이벤트를 통해 외부에 굿즈 예약 성공/실패를 알림
     */
    private void publishGoodsReservationRequestedEvent(String body) {
        java.util.UUID orderId = null;
        java.util.List<GoodsOrderReservation> createdReservations = new java.util.ArrayList<>();
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
            orderId = payload.orderId;

            if (payload.reservationItems == null || payload.reservationItems.isEmpty()) {
                log.warn("📋 [STORES] 굿즈 재고 예약 요청 항목이 없음 - orderId: {}", payload.orderId);
                return;
            }

            java.util.List<GoodsReservationItem> validItems = payload.reservationItems.stream()
                    .filter(item -> item.goodsVariantId != null && item.quantity != null && item.quantity > 0)
                    .toList();
            if (validItems.isEmpty()) {
                log.warn("📋 [STORES] 굿즈 재고 예약 요청 항목 없음 - orderId: {}", payload.orderId);
                return;
            }

            java.util.UUID resolvedPopupId = payload.popupId;
            if (resolvedPopupId == null) {
                resolvedPopupId = goodsService.resolvePopupId(validItems.get(0).goodsVariantId);
            }

            java.util.List<GoodsHoldItem> holdItems = aggregateGoodsItems(validItems);
            HoldResult holdResult = inventoryHoldService.holdGoods(payload.orderId, resolvedPopupId, holdItems);
            if (!holdResult.isSuccess()) {
                throw new RuntimeException("재고 부족으로 HOLD 실패: " + holdResult.getDetail());
            }

            for (GoodsReservationItem item : validItems) {
                GoodsOrderReservation reservation = reservationService.createGoodsReservation(
                        payload.orderId,
                        payload.orderNo,
                        resolvedPopupId,
                        item.goodsVariantId,
                        item.quantity
                );
                createdReservations.add(reservation);

                log.info("✅ [STORES] Redis HOLD 완료 - orderId: {}, goodsVariantId: {}, qty: {}",
                        payload.orderId, item.goodsVariantId, item.quantity);

                storeRedisEventPublisher.publishGoodsReservedEvent(
                        payload.orderId,
                        resolvedPopupId,
                        item.goodsVariantId,
                        item.quantity
                );
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] 굿즈 재고 예약 요청 처리 실패 - body: {}, error: {}",
                    body, e.getMessage(), e);
            if (orderId != null) {
                try {
                    inventoryHoldService.releaseHold(orderId);
                } catch (Exception releaseError) {
                    log.error("🚨 [STORES] HOLD 복구 실패 - orderId: {}, error: {}",
                            orderId, releaseError.getMessage(), releaseError);
                }
            }
            for (GoodsOrderReservation reservation : createdReservations) {
                reservationService.updateStatus(reservation, ReservationStatus.FAILED, e.getMessage());
                storeRedisEventPublisher.publishGoodsReservationFailedEvent(
                        reservation.getOrderId(),
                        reservation.getPopupId(),
                        reservation.getGoodsVariantId(),
                        reservation.getQuantity(),
                        0,
                        e.getMessage()
                );
            }
        }
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

    private List<GoodsHoldItem> aggregateGoodsItems(List<GoodsReservationItem> items) {
        Map<UUID, Integer> aggregated = new LinkedHashMap<>();
        for (GoodsReservationItem item : items) {
            aggregated.merge(item.goodsVariantId, item.quantity, Integer::sum);
        }
        return aggregated.entrySet().stream()
                .map(entry -> new GoodsHoldItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
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
