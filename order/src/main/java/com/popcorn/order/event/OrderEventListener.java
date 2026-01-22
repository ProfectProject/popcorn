package com.popcorn.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// TODO: Kafka Listener로 대체 예정
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderStatus;
import com.popcorn.order.entity.OrderItem;
import com.popcorn.order.entity.OrderItemType;
import com.popcorn.order.repository.OrderRepository;
import com.popcorn.order.repository.OrderItemRepository;
import com.popcorn.order.service.OrderService;
import com.popcorn.order.client.StoreClient;

import java.util.UUID;
import java.util.List;

/**
 * 주문 이벤트 리스너
 *
 * [역할]
 * - 내부 이벤트 및 외부 이벤트(RabbitMQ) 수신 처리
 * - Store 모듈의 재고 차감 결과 처리
 * - Payment 모듈의 결제 상태 변경 처리
 * - 보상 트랜잭션 처리
 *
 * [이벤트 수신 소스]
 * - Store 모듈: 재고 차감 성공/실패
 * - Payment 모듈: 결제 완료/취소
 * - 내부: 주문 상태 변경
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderService orderService;
    private final OrderEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final StoreClient storeClient;

    /**
     * Store 모듈의 재고 차감 실패 이벤트 수신 (Kafka로 대체 예정)
     */
    // TODO: @KafkaListener로 대체
    /*
    @Transactional
    public void handleStockDeductionFailed(String message) {
        try {
            log.info("재고 차감 실패 이벤트 수신: {}", message);

            StockDeductionFailedEvent event = objectMapper.readValue(message, StockDeductionFailedEvent.class);

            // 주문 취소 처리 (보상 트랜잭션)
            handleOrderCancellationForStockFailure(event);

        } catch (Exception e) {
            log.error("재고 차감 실패 이벤트 처리 중 오류: {}", message, e);
            throw new RuntimeException("재고 차감 실패 이벤트 처리 실패", e);
        }
    }
    */

    /**
     * Store 모듈의 재고 차감 성공 이벤트 수신 (Kafka로 대체 예정)
     */
    // TODO: @KafkaListener로 대체
    /*
    @Transactional
    public void handleStockDeductionSuccess(String message) {
        try {
            log.info("재고 차감 성공 이벤트 수신: {}", message);

            // Store 모듈의 재고 차감 성공 이벤트 파싱
            StockDeductionSuccessEvent event = objectMapper.readValue(message, StockDeductionSuccessEvent.class);

            // 주문 상태를 CONFIRMED로 변경
            handleOrderConfirmation(event.getOrderId());

        } catch (Exception e) {
            log.error("재고 차감 성공 이벤트 처리 중 오류: {}", message, e);
            throw new RuntimeException("재고 차감 성공 이벤트 처리 실패", e);
        }
    }
    */

    /**
     * Payment 모듈의 결제 완료 이벤트 수신 (내부 이벤트)
     *
     * @param event 결제 완료 이벤트
     */
    @EventListener
    @Transactional
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("결제 완료 이벤트 수신 - orderId: {}", event.getOrderId());

            // 주문 상태를 PAID로 변경
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            order.updateStatus(OrderStatus.PAID);
            orderRepository.save(order);

            // 재고 차감 요청 이벤트 발행
            eventPublisher.publishOrderPaidEvent(order);

            log.info("주문 상태 PAID로 변경 및 재고 차감 요청 이벤트 발행 완료 - orderId: {}", event.getOrderId());

        } catch (Exception e) {
            log.error("결제 완료 이벤트 처리 중 오류 - orderId: {}", event.getOrderId(), e);
            throw new RuntimeException("결제 완료 이벤트 처리 실패", e);
        }
    }

    /**
     * 주문 결제 완료 이벤트 수신 -> 재고 차감 처리
     */
    @EventListener
    @Transactional
    public void handleOrderPaid(OrderPaidEvent event) {
        try {
            log.info("주문 결제 완료 이벤트 수신 - orderId: {}", event.getOrderId());

            if (event.getOrderItems() == null || event.getOrderItems().isEmpty()) {
                log.warn("재고 차감할 주문 항목이 없습니다 - orderId: {}", event.getOrderId());
                return;
            }

            List<OrderPaidEvent.OrderItemInfo> orderItems = event.getOrderItems();
            for (OrderPaidEvent.OrderItemInfo item : orderItems) {
                if (item.isGoodsItem()) {
                    if (item.getGoodsVariantId() == null || item.getQuantity() == null) {
                        log.warn("굿즈 재고 차감 스킵 - orderId: {}, goodsVariantId: {}",
                                event.getOrderId(), item.getGoodsVariantId());
                        continue;
                    }
                    log.info("굿즈 재고 차감 요청 - orderId: {}, popupId: {}, goodsVariantId: {}, quantity: {}",
                            event.getOrderId(), event.getPopupId(), item.getGoodsVariantId(), item.getQuantity());
                    storeClient.completeGoodsReservation(
                            event.getPopupId(),
                            item.getGoodsVariantId(),
                            item.getQuantity()
                    );
                    log.info("굿즈 재고 차감 완료 - orderId: {}, goodsVariantId: {}, quantity: {}",
                            event.getOrderId(), item.getGoodsVariantId(), item.getQuantity());
                } else {
                    log.info("예약 항목은 별도 재고 처리 대상입니다 - orderId: {}", event.getOrderId());
                }
            }
        } catch (Exception e) {
            log.error("재고 차감 처리 실패 - orderId: {}, error: {}",
                    event.getOrderId(), e.getMessage(), e);
            eventPublisher.publishStockDeductionFailedEvent(
                    event.getOrderId(),
                    "재고 차감 실패: " + e.getMessage()
            );
        }
    }

    /**
     * 재고 차감 실패로 인한 주문 취소 처리 (보상 트랜잭션)
     *
     * @param event 재고 차감 실패 이벤트
     */
    private void handleOrderCancellationForStockFailure(StockDeductionFailedEvent event) {
        try {
            UUID orderId = event.getOrderId();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            // 주문이 이미 취소된 경우 무시
            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.warn("이미 취소된 주문입니다 - orderId: {}", orderId);
                return;
            }

            // 주문 취소 처리
            String cancellationReason = String.format("재고 차감 실패 - %s: %s",
                    event.getReason(), event.getDetails());

            order.updateStatus(OrderStatus.CANCELLED);
            order.setCancellationReason(cancellationReason);
            orderRepository.save(order);

            // 주문 취소 이벤트 발행 (Payment 모듈에게 결제 취소 요청)
            eventPublisher.publishOrderCancelledEvent(order, cancellationReason);

            log.info("재고 차감 실패로 인한 주문 취소 처리 완료 - orderId: {}, reason: {}",
                    orderId, cancellationReason);

        } catch (Exception e) {
            log.error("재고 차감 실패로 인한 주문 취소 처리 중 오류 - orderId: {}",
                    event.getOrderId(), e);
            throw new RuntimeException("주문 취소 처리 실패", e);
        }
    }

    /**
     * 재고 차감 성공으로 인한 주문 확정 처리
     *
     * @param orderId 주문 ID
     */
    private void handleOrderConfirmation(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + orderId));

            // 주문 상태를 COMPLETED로 변경
            order.updateStatus(OrderStatus.COMPLETED);
            orderRepository.save(order);

            // 주문 확정 완료 이벤트 발행
            eventPublisher.publishOrderCompletedEvent(order);

            log.info("재고 차감 성공으로 인한 주문 확정 처리 완료 - orderId: {}", orderId);

        } catch (Exception e) {
            log.error("주문 확정 처리 중 오류 - orderId: {}", orderId, e);
            throw new RuntimeException("주문 확정 처리 실패", e);
        }
    }

    /**
     * 내부 재고 차감 실패 이벤트 수신 (동일 서비스 내)
     *
     * @param event 재고 차감 실패 이벤트
     */
    @EventListener
    @Transactional
    public void handleInternalStockDeductionFailed(StockDeductionFailedEvent event) {
        log.info("내부 재고 차감 실패 이벤트 수신 - orderId: {}, reason: {}",
                event.getOrderId(), event.getReason());

        // 외부 이벤트 핸들러와 동일한 처리
        handleOrderCancellationForStockFailure(event);
    }

    /**
     * 주문 취소 이벤트 수신 → 재고 예약 취소 처리
     *
     * @param event 주문 취소 이벤트
     */
    @EventListener
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        try {
            log.info("주문 취소 이벤트 수신 - orderId: {}, reason: {}",
                    event.getOrderId(), event.getCancelReason());

            // 주문 정보 조회
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다: " + event.getOrderId()));

            // RESERVED 상태에서 취소된 경우에만 재고 예약 취소
            if (OrderStatus.RESERVED.equals(event.getPreviousStatus())) {
                cancelStockReservationsForOrder(order);
                log.info("재고 예약 취소 완료 - orderId: {}", event.getOrderId());
            } else {
                log.info("재고 예약 취소 불필요 - orderId: {}, previousStatus: {}",
                        event.getOrderId(), event.getPreviousStatus());
            }

        } catch (Exception e) {
            log.error("주문 취소 처리 중 오류 - orderId: {}, error: {}",
                    event.getOrderId(), e.getMessage(), e);
            // 주문 취소 자체는 성공했으므로 재고 예약 취소 실패는 로그만 남김
        }
    }

    /**
     * 주문의 모든 굿즈 항목에 대한 재고 예약을 취소합니다.
     *
     * @param order 재고 예약을 취소할 주문
     */
    private void cancelStockReservationsForOrder(Order order) {
        log.info("주문 재고 예약 취소 시작 - 주문번호: {}", order.getOrderNo());

        // 주문 항목들 조회 (order 객체에 포함되어 있지 않을 수 있음)
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());

        // 굿즈 항목만 필터링
        List<OrderItem> goodsItems = orderItems.stream()
                .filter(item -> OrderItemType.GOODS.equals(item.getOrderItemType()))
                .toList();

        if (goodsItems.isEmpty()) {
            log.info("굿즈 항목이 없어 재고 예약 취소를 건너뜁니다 - 주문번호: {}", order.getOrderNo());
            return;
        }

        // 각 굿즈 항목에 대해 재고 예약 취소
        for (OrderItem item : goodsItems) {
            if (item.getGoodsVariantId() == null) {
                log.warn("굿즈 변형 ID가 없어 재고 예약 취소를 건너뜁니다 - 주문번호: {}, 항목ID: {}",
                        order.getOrderNo(), item.getId());
                continue;
            }

            try {
                log.info("굿즈 재고 예약 취소 시도 - 주문번호: {}, 굿즈변형ID: {}, 수량: {}",
                        order.getOrderNo(), item.getGoodsVariantId(), item.getQty());

                storeClient.cancelGoodsReservation(
                        order.getPopupId(),
                        item.getGoodsVariantId(),
                        item.getQty()
                );

                log.info("굿즈 재고 예약 취소 성공 - 주문번호: {}, 굿즈변형ID: {}",
                        order.getOrderNo(), item.getGoodsVariantId());

            } catch (Exception e) {
                log.error("굿즈 재고 예약 취소 실패 - 주문번호: {}, 굿즈변형ID: {}, 에러: {}",
                        order.getOrderNo(), item.getGoodsVariantId(), e.getMessage(), e);
                // 개별 항목 실패는 로그만 남기고 계속 진행
            }
        }

        log.info("주문 재고 예약 취소 완료 - 주문번호: {}", order.getOrderNo());
    }
}
