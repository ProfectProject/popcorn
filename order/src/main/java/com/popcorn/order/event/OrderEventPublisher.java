package com.popcorn.order.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.popcorn.order.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주문 이벤트 발행자
 *
 * [역할]
 * - 주문 관련 이벤트를 내부 및 외부 시스템으로 발행
 * - 내부: Spring Events (동일 서비스 내)
 * - 외부: RabbitMQ (다른 마이크로서비스)
 *
 * [이벤트 종류]
 * - OrderPaidEvent: 결제 완료 시 재고 차감 요청
 * - OrderCancelledEvent: 주문 취소 시 재고 복원 요청
 * - OrderCompletedEvent: 주문 완료 시 알림 발송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final RedisEventPublisher redisEventPublisher;

    /**
     * 주문 결제 완료 이벤트 발행
     *
     * @param order 결제 완료된 주문
     */
    public void publishOrderPaidEvent(Order order) {
        try {
            // 1. 이벤트 객체 생성
            OrderPaidEvent event = OrderPaidEvent.createEvent(order);

            log.info("주문 결제 완료 이벤트 발행 - orderId: {}, orderNo: {}, amount: {}",
                    event.getOrderId(), event.getOrderNo(), event.getTotalAmount());

            // 2. 내부 이벤트 발행 (동일 서비스 내 처리)
            applicationEventPublisher.publishEvent(event);

            // 3. Redis 이벤트 발행 (Store 서비스 연동)
            redisEventPublisher.publishOrderPaidEvent(event);

            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("주문 결제 완료 이벤트 발행 실패 - orderId: {}", order.getId(), e);
            // 이벤트 발행 실패는 주문 프로세스를 중단시키지 않음 (최종 일관성)
        }
    }

    /**
     * 굿즈 재고 예약 요청 이벤트 발행
     *
     * @param order 주문 정보
     * @param reservationItems 예약 요청 항목들
     */
    public void publishGoodsReservationRequestedEvent(Order order,
                                                      java.util.List<GoodsReservationRequestedEvent.ReservationItem> reservationItems) {
        try {
            GoodsReservationRequestedEvent event = GoodsReservationRequestedEvent.create(
                    order.getId(),
                    order.getOrderNo(),
                    order.getPopupId(),
                    reservationItems
            );

            log.info("굿즈 재고 예약 요청 이벤트 발행 - orderId: {}, items: {}",
                    event.getOrderId(), event.getReservationItems().size());

            redisEventPublisher.publishGoodsReservationRequestedEvent(event);

        } catch (Exception e) {
            log.error("굿즈 재고 예약 요청 이벤트 발행 실패 - orderId: {}", order.getId(), e);
            // 이벤트 발행 실패는 주문 프로세스를 중단시키지 않음
        }
    }

    /**
     * 결제 생성 요청 이벤트 발행
     *
     * @param order 주문 정보
     * @param paymentMethod 결제 수단
     */
    public void publishPaymentCreateRequestedEvent(Order order, String paymentMethod) {
        try {
            String eventId = java.util.UUID.randomUUID().toString();

            log.info("결제 생성 요청 이벤트 발행 - orderId: {}, paymentMethod: {}",
                    order.getId(), paymentMethod);

            String paymentKey = "order:" + order.getId();

            redisEventPublisher.publishPaymentCreateRequestedEvent(
                    eventId,
                    order.getId(),
                    order.getOrderNo(),
                    order.getTotalAmount(),
                    paymentMethod,
                    order.getCustomerId(),
                    paymentKey
            );

        } catch (Exception e) {
            log.error("결제 생성 요청 이벤트 발행 실패 - orderId: {}", order.getId(), e);
        }
    }

    /**
     * 주문 취소 이벤트 발행
     *
     * @param order 취소된 주문
     * @param reason 취소 사유
     */
    public void publishOrderCancelledEvent(Order order, String reason) {
        try {
            OrderCancelledEvent event = OrderCancelledEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .customerId(order.getCustomerId())
                    .popupId(order.getPopupId())
                    .reason(reason)
                    .cancelledAt(java.time.LocalDateTime.now())
                    .eventTime(java.time.LocalDateTime.now())
                    .build();

            log.info("주문 취소 이벤트 발행 - orderId: {}, reason: {}",
                    event.getOrderId(), event.getReason());

            // 내부 이벤트 발행
            applicationEventPublisher.publishEvent(event);
            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("주문 취소 이벤트 발행 실패 - orderId: {}", order.getId(), e);
        }
    }

    /**
     * 재고 차감 실패 이벤트 발행
     *
     * @param orderId 주문 ID
     * @param reason 실패 사유
     */
    public void publishStockDeductionFailedEvent(java.util.UUID orderId, String reason) {
        try {
            StockDeductionFailedEvent event = StockDeductionFailedEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .orderId(orderId)
                    .reason(reason)
                    .failedAt(java.time.LocalDateTime.now())
                    .eventTime(java.time.LocalDateTime.now())
                    .build();

            log.warn("재고 차감 실패 이벤트 발행 - orderId: {}, reason: {}",
                    orderId, reason);

            // 내부 이벤트 발행
            applicationEventPublisher.publishEvent(event);
            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 발행 실패 - orderId: {}", orderId, e);
        }
    }

    // TODO: Kafka 이벤트 발행 메서드로 대체 예정

    /**
     * 주문 완료 이벤트 발행 (알림 발송용)
     *
     * @param order 완료된 주문
     */
    public void publishOrderCompletedEvent(Order order) {
        try {
            OrderCompletedEvent event = OrderCompletedEvent.builder()
                    .eventId(java.util.UUID.randomUUID().toString())
                    .orderId(order.getId())
                    .orderNo(order.getOrderNo())
                    .customerId(order.getCustomerId())
                    .popupId(order.getPopupId())
                    .orderDate(order.getCreatedAt())
                    .finalAmount(order.getTotalAmount())
                    .itemCount(order.getTotalQuantity())
                    .completedAt(java.time.LocalDateTime.now())
                    .eventTime(java.time.LocalDateTime.now())
                    .build();

            log.info("주문 완료 이벤트 발행 - orderId: {}", event.getOrderId());

            // 내부 이벤트 발행
            applicationEventPublisher.publishEvent(event);
            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("주문 완료 이벤트 발행 실패 - orderId: {}", order.getId(), e);
        }
    }

    /**
     * 재고 예약 성공 이벤트 발행
     *
     * @param order 주문 정보
     * @param reservedItems 예약된 재고 항목들
     */
    public void publishStockReservedEvent(Order order,
                                        java.util.List<StockReservedEvent.ReservedStockItem> reservedItems) {
        try {
            StockReservedEvent event = StockReservedEvent.create(
                    order.getId(),
                    order.getOrderNo(),
                    order.getPopupId(),
                    order.getCustomerId(),
                    reservedItems
            );

            log.info("재고 예약 성공 이벤트 발행 - orderId: {}, items: {}",
                    event.getOrderId(), event.getReservedItems().size());

            // 내부 이벤트 발행 (Spring Events)
            applicationEventPublisher.publishEvent(event);
            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("재고 예약 성공 이벤트 발행 실패 - orderId: {}", order.getId(), e);
        }
    }

    /**
     * 재고 예약 실패 이벤트 발행
     *
     * @param order 주문 정보
     * @param failedItems 실패한 재고 항목들
     * @param failureReason 실패 이유
     */
    public void publishStockReservationFailedEvent(Order order,
                                                  java.util.List<StockReservationFailedEvent.FailedStockItem> failedItems,
                                                  String failureReason) {
        try {
            StockReservationFailedEvent event = StockReservationFailedEvent.create(
                    order.getId(),
                    order.getOrderNo(),
                    order.getPopupId(),
                    order.getCustomerId(),
                    failedItems,
                    failureReason
            );

            log.info("재고 예약 실패 이벤트 발행 - orderId: {}, reason: {}",
                    event.getOrderId(), event.getFailureReason());

            // 내부 이벤트 발행 (Spring Events)
            applicationEventPublisher.publishEvent(event);
            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("재고 예약 실패 이벤트 발행 실패 - orderId: {}", order.getId(), e);
        }
    }

    /**
     * 재고 차감 요청 이벤트 발행
     *
     * @param orderId 주문 ID
     * @param orderNo 주문 번호
     * @param popupId 팝업 ID
     * @param deductionItems 차감 항목들
     */
    public void publishStockDeductionRequestedEvent(java.util.UUID orderId, String orderNo,
                                                   java.util.UUID popupId,
                                                   java.util.List<StockDeductionRequestedEvent.StockDeductionItem> deductionItems) {
        try {
            log.info("📦 [ORDER] 재고 차감 요청 이벤트 발행 시작 - orderId: {}, 항목 수: {}", orderId, deductionItems.size());

            StockDeductionRequestedEvent event = StockDeductionRequestedEvent.create(
                    orderId, orderNo, popupId, deductionItems
            );

            // 1. 내부 이벤트 발행 (Spring Events)
            applicationEventPublisher.publishEvent(event);

            // 2. Redis Stream 이벤트 발행 (Store 서비스로 전송)
            redisEventPublisher.publishStockDeductionRequestedEvent(event);

            log.info("📦✅ [ORDER] 재고 차감 요청 이벤트 발행 완료 - orderId: {}, eventId: {}",
                    orderId, event.getEventId());

        } catch (Exception e) {
            log.error("📦❌ [ORDER] 재고 차감 요청 이벤트 발행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
        }
    }
}
