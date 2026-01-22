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
 *
 * [초보자 가이드]
 * 주문이 성공적으로 생성된 후 클라이언트에게 반환되는 정보입니다.
 * 생성된 주문의 상세 정보와 주문 항목들이 포함됩니다.
 *
 * 중요한 패턴:
 * - Builder 패턴: 객체 생성을 깔끔하게 처리
 * - Static factory method: Entity → DTO 변환을 담당
 * - Nested class: 관련된 클래스를 내부에 정의
 */
@Getter
@Builder
public class CreateOrderResponse {

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

    /**
     * 주문 항목 응답 DTO (중첩 클래스)
     *
     * [초보자 가이드]
     * static nested class: 외부 클래스의 인스턴스 없이도 독립적으로 사용 가능
     * CreateOrderResponse.OrderItemResponse로 접근
     */
    @Getter
    @Builder
    public static class OrderItemResponse {

        /** 주문 항목 ID */
        private final UUID itemId;

        /** 주문 항목 타입 - "RESERVATION" 또는 "GOODS" */
        private final String orderItemType;

        /** 수량 */
        private final Integer qty;

        /** 단가 (원) */
        private final Integer unitPrice;

        /** 라인 금액 (단가 × 수량) */
        private final Integer lineAmount;
    }

    /**
     * 결제 정보 응답 DTO (중첩 클래스)
     *
     * [초보자 가이드]
     * 주문 생성 후 결제 프로세스가 시작되면서 생성되는 결제 관련 정보
     * 클라이언트가 결제를 진행할 수 있도록 필요한 정보들을 포함
     */
    @Getter
    @Builder
    public static class PaymentInfo {

        /** 결제 ID - Payment 서비스에서 생성한 고유 식별자 */
        private final UUID paymentId;

        /** 결제 상태 - "READY", "PENDING", "COMPLETED" 등 */
        private final String paymentStatus;

        /** 결제 방법 - "CARD", "TRANSFER", "MOBILE_PHONE" 등 */
        private final String paymentMethod;

        /** 결제 URL - 고객이 결제를 진행할 수 있는 링크 (있는 경우만) */
        private final String paymentUrl;

        /** 결제 만료 시간 - 이 시간까지만 결제 가능 */
        private final LocalDateTime expiresAt;

        /** 결제 메시지 - 고객에게 표시할 안내 메시지 */
        private final String message;
    }

    /**
     * Order 엔티티로부터 CreateOrderResponse 생성
     * @param order 주문 엔티티
     * @return 변환된 응답 DTO
     *
     * [초보자 가이드]
     * Static factory method 패턴:
     * - 생성자 대신 의미있는 이름의 static 메서드로 객체 생성
     * - fromOrder()라는 이름으로 어떤 변환인지 명확히 표현
     * - Stream API로 컬렉션 변환 처리
     */
    public static CreateOrderResponse fromOrder(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(CreateOrderResponse::fromOrderItem)  // 메서드 레퍼런스 사용
                .toList();

        return CreateOrderResponse.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .orderType(order.getOrderType().name())  // Enum → String 변환
                .status(order.getStatus().name())
                .popupId(order.getPopupId())
                .totalAmount(order.getTotalAmount())
                .cancelableUntil(order.getCancelableUntil())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .paymentInfo(null) // 기본적으로는 결제 정보 없음
                .build();
    }

    /**
     * 결제 정보를 포함한 CreateOrderResponse 생성
     * @param order 주문 엔티티
     * @param paymentId 결제 ID
     * @param paymentStatus 결제 상태
     * @param paymentMethod 결제 방법
     * @param paymentUrl 결제 URL
     * @param expiresAt 결제 만료 시간
     * @param message 결제 메시지
     * @return 결제 정보가 포함된 응답 DTO
     */
    public static CreateOrderResponse fromOrderWithPayment(Order order,
                                                          UUID paymentId,
                                                          String paymentStatus,
                                                          String paymentMethod,
                                                          String paymentUrl,
                                                          LocalDateTime expiresAt,
                                                          String message) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(CreateOrderResponse::fromOrderItem)
                .toList();

        PaymentInfo paymentInfo = PaymentInfo.builder()
                .paymentId(paymentId)
                .paymentStatus(paymentStatus)
                .paymentMethod(paymentMethod)
                .paymentUrl(paymentUrl)
                .expiresAt(expiresAt)
                .message(message)
                .build();

        return CreateOrderResponse.builder()
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

    /**
     * OrderItem 엔티티로부터 OrderItemResponse 생성
     * @param item 주문 항목 엔티티
     * @return 변환된 주문 항목 응답 DTO
     *
     * [초보자 가이드]
     * private static: 이 클래스 내부에서만 사용하는 헬퍼 메서드
     * Entity → DTO 변환 로직을 캡슐화
     */
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
