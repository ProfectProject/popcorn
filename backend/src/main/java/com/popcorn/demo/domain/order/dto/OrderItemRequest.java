package com.popcorn.demo.domain.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 주문 항목 요청 DTO
 * - 주문 생성 시 개별 항목 데이터
 * - 예약형/굿즈형 항목을 구분하여 처리
 * - 타입별 필수 필드가 다름 (조건부 검증 필요)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemRequest {
    
    // ========================= 공통 필드 =========================
    
    /**
     * 주문 항목 타입 (RESERVATION / MERCH)
     * - orderType과 일치해야 함
     */
    @NotBlank(message = "주문 항목 타입은 필수입니다.")
    private String orderItemType;
    
    /**
     * 수량 (1 이상)
     */
    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Integer qty;
    
    // ========================= 예약형 전용 필드 =========================
    
    /**
     * 세션 ID (예약형일 때 필수)
     * - orderItemType=RESERVATION일 때 필수
     */
    private Long sessionId;
    
    /**
     * 옵션 ID (예약형일 때 필수)  
     * - orderItemType=RESERVATION일 때 필수
     */
    private Long optionId;
    
    // ========================= 구매형 전용 필드 =========================
    
    /**
     * 상품 변형 ID (구매형일 때 필수)
     * - orderItemType=MERCH일 때 필수
     */
    private Long merchVariantId;
    
    // ========================= 검증 메서드 =========================
    
    /**
     * 예약형 주문 항목인지 확인
     * @return 예약형이면 true
     */
    public boolean isReservationType() {
        return "RESERVATION".equals(orderItemType);
    }
    
    /**
     * 굿즈형 주문 항목인지 확인
     * @return 굿즈형이면 true
     */
    public boolean isMerchType() {
        return "MERCH".equals(orderItemType);
    }
    
    /**
     * 예약형 주문 항목의 필수 필드 검증
     * @return 예약형 필수 필드가 모두 있으면 true
     */
    public boolean isValidReservationItem() {
        return isReservationType() && 
               sessionId != null && 
               optionId != null &&
               qty != null && qty > 0;
    }
    
    /**
     * 굿즈형 주문 항목의 필수 필드 검증
     * @return 굿즈형 필수 필드가 모두 있으면 true
     */
    public boolean isValidMerchItem() {
        return isMerchType() && 
               merchVariantId != null &&
               qty != null && qty > 0;
    }
    
    /**
     * 타입에 맞는 필수 필드 검증
     * @return 해당 타입의 필수 필드가 모두 있으면 true
     */
    public boolean hasRequiredFields() {
        if (isReservationType()) {
            return isValidReservationItem();
        } else if (isMerchType()) {
            return isValidMerchItem();
        }
        return false;  // 알 수 없는 타입
    }
    
    /**
     * 불필요한 필드가 있는지 확인
     * @return 불필요한 필드가 있으면 true
     */
    public boolean hasUnnecessaryFields() {
        if (isReservationType()) {
            // 예약형인데 굿즈 필드가 있으면 불필요
            return merchVariantId != null;
        } else if (isMerchType()) {
            // 굿즈형인데 예약 필드가 있으면 불필요
            return sessionId != null || optionId != null;
        }
        return false;
    }
    
    /**
     * 세션 옵션 ID 조합 생성 (예약형용)
     * - sessionId와 optionId를 조합한 고유 식별자
     * @return 세션 옵션 조합 ID 또는 null
     */
    public String getSessionOptionKey() {
        if (isReservationType() && sessionId != null && optionId != null) {
            return sessionId + "-" + optionId;
        }
        return null;
    }
    
    /**
     * 재고 추적용 식별자 반환
     * @return 예약형: 세션옵션키, 굿즈형: 상품변형ID
     */
    public String getStockIdentifier() {
        if (isReservationType()) {
            return getSessionOptionKey();
        } else if (isMerchType()) {
            return "VARIANT_" + merchVariantId;
        }
        return null;
    }
    
    /**
     * 디스플레이용 설명 생성
     * @return 항목 설명 문자열
     */
    public String getDisplayDescription() {
        if (isReservationType()) {
            return String.format("예약 (세션: %d, 옵션: %d) x %d개", sessionId, optionId, qty);
        } else if (isMerchType()) {
            return String.format("굿즈 (변형: %d) x %d개", merchVariantId, qty);
        }
        return String.format("항목 (%s) x %d개", orderItemType, qty);
    }
}
