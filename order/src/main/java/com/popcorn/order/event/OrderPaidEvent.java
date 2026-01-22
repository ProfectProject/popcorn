package com.popcorn.order.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 주문 결제 완료 이벤트
 *
 * [이벤트 발행 시점]
 * - Payment 모듈에서 결제 성공 후 Order 모듈이 주문 상태를 PAID로 변경할 때 발행
 *
 * [이벤트 수신자]
 * - Store 모듈: 재고 차감 처리
 * - Notification 모듈: 주문 확정 알림 발송
 * - Analytics 모듈: 매출 집계 처리
 *
 * [마이크로서비스 통신]
 * 이 이벤트는 비동기 메시징(RabbitMQ/Kafka)을 통해 다른 서비스로 전파됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrderPaidEvent {

    /** 이벤트 ID (추적용) */
    private String eventId;

    /** 주문 ID */
    private UUID orderId;

    /** 주문 번호 */
    private String orderNo;

    /** 고객 ID */
    private Long customerId;

    /** 팝업 ID */
    private UUID popupId;

    /** 주문 타입 */
    private String orderType;

    /** 총 결제 금액 */
    private Integer totalAmount;

    /** 주문 항목 목록 (재고 차감용) */
    private List<OrderItemInfo> orderItems;

    /** 결제 완료 시간 */
    private LocalDateTime paidAt;

    /** 이벤트 발생 시간 */
    private LocalDateTime eventTime;

    /**
     * 주문 항목 정보 (재고 차감에 필요한 정보만)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class OrderItemInfo {

        /** 주문 항목 타입 */
        private String orderItemType; // RESERVATION 또는 GOODS

        /** 수량 */
        private Integer quantity;

        /** 세션 ID (예약형인 경우) */
        private UUID sessionId;

        /** 굿즈 변형 ID (구매형인 경우) */
        private UUID goodsVariantId;

        /**
         * 예약형 아이템인지 확인
         */
        public boolean isReservationItem() {
            return "RESERVATION".equals(orderItemType);
        }

        /**
         * 굿즈형 아이템인지 확인
         */
        public boolean isGoodsItem() {
            return "GOODS".equals(orderItemType);
        }

        /**
         * 재고 식별자 반환 (Store 모듈에서 재고 차감 시 사용)
         */
        public String getStockIdentifier() {
            if (isReservationItem()) {
                return "SESSION:" + sessionId;
            }
            if (isGoodsItem()) {
                return "GOODS:" + goodsVariantId;
            }
            return null;
        }
    }

    /**
     * 이벤트 생성 팩토리 메서드
     *
     * @param order 주문 엔티티
     * @return OrderPaidEvent
     */
    public static OrderPaidEvent createEvent(com.popcorn.order.entity.Order order) {
        return OrderPaidEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .customerId(order.getCustomerId())
                .popupId(order.getPopupId())
                .orderType(order.getOrderType() != null ? order.getOrderType().name() : null)
                .totalAmount(order.getTotalAmount())
                .orderItems(convertOrderItems(order.getOrderItems()))
                .paidAt(LocalDateTime.now())
                .eventTime(LocalDateTime.now())
                .build();
    }

    /**
     * 주문 항목을 이벤트용 DTO로 변환
     */
    private static List<OrderItemInfo> convertOrderItems(List<com.popcorn.order.entity.OrderItem> orderItems) {
        if (orderItems == null) {
            return List.of();
        }

        return orderItems.stream()
                .map(item -> OrderItemInfo.builder()
                        .orderItemType(item.getOrderItemType() != null ? item.getOrderItemType().name() : null)
                        .quantity(item.getQty())
                        .sessionId(item.getSessionOptionId()) // OrderItem에서는 sessionOptionId 필드명 사용
                        .goodsVariantId(item.getGoodsVariantId())
                        .build())
                .toList();
    }

    /**
     * 예약 항목들만 필터링
     */
    public List<OrderItemInfo> getReservationItems() {
        return orderItems.stream()
                .filter(OrderItemInfo::isReservationItem)
                .toList();
    }

    /**
     * 굿즈 항목들만 필터링
     */
    public List<OrderItemInfo> getGoodsItems() {
        return orderItems.stream()
                .filter(OrderItemInfo::isGoodsItem)
                .toList();
    }

    /**
     * 혼합형 주문인지 확인
     */
    public boolean isMixedOrder() {
        return "MIXED".equals(orderType);
    }

    /**
     * Store 모듈로 전송할 재고 차감 요청 데이터 생성
     */
    public StockDeductionRequest toStockDeductionRequest() {
        return StockDeductionRequest.builder()
                .orderId(orderId)
                .orderNo(orderNo)
                .popupId(popupId)
                .orderItems(orderItems)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Store 모듈로 전송할 재고 차감 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockDeductionRequest {
        private UUID orderId;
        private String orderNo;
        private UUID popupId;
        private List<OrderItemInfo> orderItems;
        private LocalDateTime requestedAt;
    }
}