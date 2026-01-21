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

            // TODO: Kafka 이벤트 발행으로 대체 예정

        } catch (Exception e) {
            log.error("주문 결제 완료 이벤트 발행 실패 - orderId: {}", order.getId(), e);
            // 이벤트 발행 실패는 주문 프로세스를 중단시키지 않음 (최종 일관성)
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
}