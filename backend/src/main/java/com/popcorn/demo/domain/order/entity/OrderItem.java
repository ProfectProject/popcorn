package com.popcorn.demo.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 주문 항목 도메인 엔티티
 * - 클린 아키텍처의 조합(Composition) 패턴을 사용
 * - 예약형/굿즈형 주문 항목을 하나의 엔티티로 처리
 * - 타입별 세부사항은 별도 클래스로 조합하여 관리 (SRP 원칙 준수)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    
    // ========================= 공통 필드 =========================
    
    /** 주문 항목 ID */
    private Long id;
    
    /** 소속 주문 ID */
    private Long orderId;
    
    /** 주문 항목 타입 (예약형/굿즈형) */
    private OrderItemType orderItemType;
    
    /** 주문 수량 (1 이상) */
    private Integer quantity;
    
    /** 단가 (원 단위) */
    private Integer unitPrice;
    
    /** 라인 금액 (단가 * 수량) */
    private Integer lineAmount;
    
    // ========================= 조합 필드 =========================
    
    /** 예약형 주문 시에만 사용되는 세부 정보 */
    private ReservationDetail reservationDetail;
    
    /** 굿즈형 주문 시에만 사용되는 세부 정보 */
    private MerchDetail merchDetail;
    
    // ========================= 팩토리 메서드 =========================
    
    /**
     * 예약형 주문 항목 생성
     * @param sessionOptionId 세션 옵션 ID (필수)
     * @param quantity 수량 (1 이상)
     * @param unitPrice 단가
     * @return 예약형 주문 항목
     */
    public static OrderItem createReservationItem(Long sessionOptionId, Integer quantity, Integer unitPrice) {
        validateQuantity(quantity);
        
        // 예약형 세부 정보 생성
        ReservationDetail reservationDetail = ReservationDetail.builder()
                .sessionOptionId(sessionOptionId)
                .build();
        
        return OrderItem.builder()
                .orderItemType(OrderItemType.RESERVATION)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineAmount(unitPrice * quantity)  // 라인 금액 자동 계산
                .reservationDetail(reservationDetail)
                .merchDetail(null)  // 굿즈 정보는 null
                .build();
    }
    
    /**
     * 굿즈형 주문 항목 생성
     * @param merchVariantId 상품 변형 ID (필수)
     * @param quantity 수량 (1 이상)
     * @param unitPrice 단가
     * @return 굿즈형 주문 항목
     */
    public static OrderItem createMerchItem(Long merchVariantId, Integer quantity, Integer unitPrice) {
        validateQuantity(quantity);
        
        // 굿즈형 세부 정보 생성
        MerchDetail merchDetail = MerchDetail.builder()
                .merchVariantId(merchVariantId)
                .build();
        
        return OrderItem.builder()
                .orderItemType(OrderItemType.MERCH)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .lineAmount(unitPrice * quantity)  // 라인 금액 자동 계산
                .reservationDetail(null)  // 예약 정보는 null
                .merchDetail(merchDetail)
                .build();
    }
    
    // ========================= 검증 메서드 =========================
    
    /**
     * 수량 검증
     * @param quantity 검증할 수량
     * @throws IllegalArgumentException 수량이 1 미만인 경우
     */
    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
        }
    }
    
    // ========================= 비즈니스 로직 메서드 =========================
    
    /**
     * 예약형 주문 항목인지 확인
     * @return 예약형이면 true, 아니면 false
     */
    public boolean isReservationType() {
        return OrderItemType.RESERVATION.equals(orderItemType) && reservationDetail != null;
    }
    
    /**
     * 굿즈형 주문 항목인지 확인
     * @return 굿즈형이면 true, 아니면 false
     */
    public boolean isMerchType() {
        return OrderItemType.MERCH.equals(orderItemType) && merchDetail != null;
    }
    
    // ========================= 편의 메서드 =========================
    
    /**
     * 세션 옵션 ID 반환 (예약형인 경우에만)
     * @return 세션 옵션 ID 또는 null
     */
    public Long getSessionOptionId() {
        return reservationDetail != null ? reservationDetail.getSessionOptionId() : null;
    }
    
    /**
     * 상품 변형 ID 반환 (굿즈형인 경우에만)
     * @return 상품 변형 ID 또는 null
     */
    public Long getMerchVariantId() {
        return merchDetail != null ? merchDetail.getMerchVariantId() : null;
    }
}
