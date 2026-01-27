package com.popcorn.checkIns.event.standard;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 표준 이벤트 타입 정의
 * 모든 마이크로서비스에서 사용되는 표준 이벤트 타입들
 */
@Getter
@RequiredArgsConstructor
public enum StandardEventType {

    // === 📦 Order Events ===
    ORDER_CREATED("ORDER_CREATED", "주문 생성"),
    ORDER_PAID("ORDER_PAID", "주문 결제 완료"),
    ORDER_CANCELLED("ORDER_CANCELLED", "주문 취소"),
    ORDER_COMPLETED("ORDER_COMPLETED", "주문 완료"),

    // === 💳 Payment Events ===
    PAYMENT_APPROVED("PAYMENT_APPROVED", "결제 승인"),
    PAYMENT_FAILED("PAYMENT_FAILED", "결제 실패"),
    PAYMENT_CANCELLED("PAYMENT_CANCELLED", "결제 취소"),
    PAYMENT_COMPLETED("PAYMENT_COMPLETED", "결제 완료"),

    // === 🏪 Store Events ===
    POPUP_CREATED("POPUP_CREATED", "팝업 생성"),
    POPUP_STATUS_UPDATED("POPUP_STATUS_UPDATED", "팝업 상태 변경"),
    POPUP_UPDATED("POPUP_UPDATED", "팝업 정보 수정"),
    POPUP_DELETED("POPUP_DELETED", "팝업 삭제"),

    // === 📱 CheckIns Events ===
    QR_GENERATED("QR_GENERATED", "QR 코드 생성"),
    QR_VERIFIED("QR_VERIFIED", "QR 코드 검증"),
    CHECKIN_CREATED("CHECKIN_CREATED", "체크인 생성"),
    CHECKIN_COMPLETED("CHECKIN_COMPLETED", "체크인 완료"),

    // === 👥 User Events ===
    USER_CREATED("USER_CREATED", "사용자 생성"),
    USER_UPDATED("USER_UPDATED", "사용자 정보 수정"),
    USER_DELETED("USER_DELETED", "사용자 삭제");

    private final String value;
    private final String description;
}