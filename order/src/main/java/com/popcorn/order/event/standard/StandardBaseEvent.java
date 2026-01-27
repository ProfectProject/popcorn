package com.popcorn.order.event.standard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 표준 이벤트 기본 구조
 * 모든 표준 이벤트가 상속해야 하는 기본 클래스
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class StandardBaseEvent {

    // === 📦 공통 Event Payload (모든 이벤트 공통) ===

    /**
     * 이벤트 고유 ID
     */
    private String eventId;

    /**
     * 이벤트 타입 (ORDER_CREATED, PAYMENT_APPROVED 등)
     */
    private StandardEventType eventType;

    /**
     * 이벤트 발생 시간
     */
    private LocalDateTime occurredAt;

    /**
     * 이벤트 생산자 서비스 (order-service, payment-service 등)
     */
    private String producer;

    // === 비즈니스 공통 필드 ===

    /**
     * 주문 ID
     */
    private UUID orderId;

    /**
     * 주문 번호
     */
    private String orderNo;

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 상점 ID
     */
    private UUID storeId;

    /**
     * 팝업 ID
     */
    private UUID popupId;

    /**
     * 예약 포함 여부
     */
    private Boolean hasReservation;

    /**
     * 굿즈 포함 여부
     */
    private Boolean hasGoods;

    /**
     * 라인 아이템 목록 (주문 내 개별 항목들)
     */
    private List<EventLineItem> lines;

    /**
     * 기본값으로 현재 시간과 UUID 설정
     */
    protected void setDefaults() {
        if (this.eventId == null) {
            this.eventId = UUID.randomUUID().toString();
        }
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
        }
    }
}