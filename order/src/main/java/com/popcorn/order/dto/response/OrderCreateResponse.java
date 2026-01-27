package com.popcorn.order.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.popcorn.order.entity.Order;
import com.popcorn.order.entity.OrderItem;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 생성 응답 DTO
 */
@Getter
@Builder
public class OrderCreateResponse {

    /** 주문 ID - 시스템에서 생성된 고유 식별자 */
    private final UUID orderId;

    /** 주문 번호 - 사용자에게 표시되는 주문 번호 */
    private final String orderNo;

    /** 주문 타입 - "RESERVATION" 또는 "PURCHASE" */
    private final String orderType;

    /** 주문 상태 - "REQUESTED", "ACCEPTED" 등 */
    private final String status;

    /** 팝업 ID - 관련된 팝업 이벤트 */
    private final UUID popupId;

    /** 총 주문 금액 (원) */
    private final Integer totalAmount;

    /** 취소 가능 시한 - 이 시간까지만 주문 취소 가능 */
    private final LocalDateTime cancelableUntil;

    /** 주문 생성 시간 */
    private final LocalDateTime createdAt;

    /** 주문 항목 목록 */
    private final List<OrderItemResponse> items;

    /** 결제 정보 - 주문 생성 후 결제 프로세스 시작 시 포함 */
    private final PaymentInfo paymentInfo;

    @Getter
    @Builder
    public static class OrderItemResponse {
        private final UUID itemId;
        private final String orderItemType;
        private final Integer qty;
        private final Integer unitPrice;
        private final Integer lineAmount;
    }

    @Getter
    @Builder
    public static class PaymentInfo {
        private final UUID paymentId;
        private final String paymentStatus;
        private final String paymentMethod;
        private final String paymentUrl;
        private final LocalDateTime expiresAt;
        private final String message;
    }

    public static OrderCreateResponse fromOrder(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderCreateResponse::fromOrderItem)
                .toList();

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .popupId(order.getPopupId())
                .totalAmount(order.getTotalAmount())
                .cancelableUntil(order.getCancelableUntil())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .paymentInfo(null)
                .build();
    }

    public static OrderCreateResponse fromOrderWithPayment(Order order,
                                                           UUID paymentId,
                                                           String paymentStatus,
                                                           String paymentMethod,
                                                           String paymentUrl,
                                                           LocalDateTime expiresAt,
                                                           String message) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(OrderCreateResponse::fromOrderItem)
                .toList();

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .paymentId(paymentId)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .paymentUrl(paymentUrl)
                .expiresAt(expiresAt)
                .message(message)
                .build();

        return OrderCreateResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .orderType(order.getOrderType().name())
                .status(order.getStatus().name())
                .popupId(order.getPopupId())
                .totalAmount(order.getTotalAmount())
                .cancelableUntil(order.getCancelableUntil())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .paymentInfo(paymentInfo)
                .build();
    }

    private static OrderItemResponse fromOrderItem(OrderItem item) {
        return OrderItemResponse.builder()
                .itemId(item.getId())
                .orderItemType(item.getOrderItemType().name())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .lineAmount(item.getLineAmount())
                .build();
    }
}
