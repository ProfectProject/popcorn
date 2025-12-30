package com.popcorn.demo.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 주문 생성 요청 DTO
 * - API 요청으로 받는 주문 생성 데이터
 * - Jakarta Validation 적용으로 입력 검증
 * - 예약형/구매형 주문 모두 지원
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    
    // ========================= 기본 주문 정보 =========================
    
    /**
     * 주문 타입 (RESERVATION / PURCHASE)
     * - RESERVATION: 예약형 주문
     * - PURCHASE: 구매형 주문
     */
    @NotBlank(message = "주문 타입은 필수입니다.")
    private String orderType;
    
    /**
     * 주문 대상 스토어 ID
     */
    @NotNull(message = "스토어 ID는 필수입니다.")
    @Positive(message = "스토어 ID는 양수여야 합니다.")
    private Long storeId;
    
    /**
     * 주문 기준 상품 ID (행사/굿즈)
     */
    @NotNull(message = "상품 ID는 필수입니다.")
    @Positive(message = "상품 ID는 양수여야 합니다.")
    private Long productId;
    
    /**
     * 연동할 예약 ID (PURCHASE에서 체크인 연동용, 선택)
     */
    private Long reservationId;
    
    // ========================= 주문 항목 정보 =========================
    
    /**
     * 주문 아이템 목록 (최소 1개)
     */
    @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<OrderItemRequest> items;
    
    // ========================= 배송 정보 =========================
    
    /**
     * 배송 주소 (PURCHASE면 필수, RESERVATION이면 선택)
     */
    @Valid
    private AddressRequest address;
    
    // ========================= 검증 메서드 =========================
    
    /**
     * 예약형 주문인지 확인
     * @return 예약형이면 true
     */
    public boolean isReservationType() {
        return "RESERVATION".equals(orderType);
    }

    /**
     * 구매형 주문인지 확인
     * @return 구매형이면 true
     */
    public boolean isPurchaseType() {
        return "PURCHASE".equals(orderType);
    }
    
    /**
     * 주문 항목들의 타입 일관성 검증
     * @return 모든 항목이 같은 타입이면 true
     */
    public boolean hasConsistentItemTypes() {
        if (items == null || items.isEmpty()) {
            return false;
        }
        
        String expectedItemType = isReservationType() ? "RESERVATION" : "MERCH";
        
        return items.stream()
                .allMatch(item -> expectedItemType.equals(item.getOrderItemType()));
    }
    
    /**
     * 예약형 주문의 필수 필드 검증
     * @return 예약형 주문의 모든 필수 필드가 있으면 true
     */
    public boolean isValidReservationRequest() {
        if (!isReservationType()) {
            return false;
        }
        
        return items.stream()
                .allMatch(item -> 
                    "RESERVATION".equals(item.getOrderItemType()) &&
                    item.getSessionId() != null &&
                    item.getOptionId() != null
                );
    }
    
    /**
     * 구매형 주문의 필수 필드 검증
     * @return 구매형 주문의 모든 필수 필드가 있으면 true
     */
    public boolean isValidPurchaseRequest() {
        if (!isPurchaseType()) {
            return false;
        }
        
        boolean hasValidItems = items.stream()
                .allMatch(item -> 
                    "MERCH".equals(item.getOrderItemType()) &&
                    item.getMerchVariantId() != null
                );
        
        // PURCHASE면 address.address1이 필수
        boolean hasValidAddress = address != null && address.getAddress1() != null && !address.getAddress1().trim().isEmpty();
        
        return hasValidItems && hasValidAddress;
    }
    
    /**
     * 총 주문 항목 수량 계산
     * @return 모든 항목의 수량 합계
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(OrderItemRequest::getQty)
                .sum();
    }
}
