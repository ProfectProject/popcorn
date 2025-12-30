package com.popcorn.demo.domain.order.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * 주문 도메인 엔티티
 * - 주문의 핵심 비즈니스 로직을 담당
 * - 예약형/구매형 주문을 통합 관리
 * - 주문 상태 변화와 취소 정책 등 비즈니스 규칙 포함
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    
    // ========================= 기본 필드 =========================
    
    /** 주문 ID */
    private Long id;
    
    /** 주문 번호 (고유 식별자) */
    private String orderNo;
    
    /** 고객 ID */
    private Long customerId;
    
    /** 스토어 ID */
    private Long storeId;
    
    /** 상품 ID */
    private Long productId;
    
    /** 주문 타입 (예약형/구매형) */
    private OrderType orderType;
    
    /** 주문 상태 */
    private OrderStatus status;
    
    /** 취소 가능 시간 */
    private LocalDateTime cancelableUntil;
    
    /** 총 주문 금액 (원 단위) */
    private Integer totalAmount;
    
    /** 주문 항목 목록 */
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
    
    /** 생성 시간 */
    private LocalDateTime createdAt;
    
    /** 수정 시간 */
    private LocalDateTime updatedAt;
    
    // ========================= 주문 번호 생성 메서드 =========================
    
    /**
     * 주문 번호 생성 
     * 형식: O + YYYYMMDD + 6자리 시퀀스
     * 예: O20251230-000001
     */
    public static String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 실제 구현에서는 DB sequence나 Redis counter 사용 권장
        long sequence = System.currentTimeMillis() % 1000000;
        return "O" + dateStr + "-" + String.format("%06d", sequence);
    }
    
    // ========================= 팩토리 메서드 =========================
    
    /**
     * 예약형 주문 생성
     * @param customerId 고객 ID
     * @param storeId 스토어 ID  
     * @param productId 상품 ID
     * @param items 주문 항목 목록
     * @return 예약형 주문 엔티티
     * @throws IllegalArgumentException 검증 실패 시
     */
    public static Order createReservationOrder(Long customerId, Long storeId, Long productId, List<OrderItem> items) {
        validateReservationItems(items);
        validateBasicFields(customerId, storeId, productId);
        
        int totalAmount = calculateTotalAmount(items);
        
        // 취소 가능 시간: 예약 정책 - 생성 후 24시간
        LocalDateTime cancelableUntil = LocalDateTime.now().plusHours(24);
        
        return Order.builder()
                .orderNo(generateOrderNo())
                .customerId(customerId)
                .storeId(storeId)
                .productId(productId)
                .orderType(OrderType.RESERVATION)
                .status(OrderStatus.REQUESTED)
                .cancelableUntil(cancelableUntil)
                .totalAmount(totalAmount)
                .items(new ArrayList<>(items))  // 방어적 복사
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 구매형 주문 생성
     * @param customerId 고객 ID
     * @param storeId 스토어 ID
     * @param productId 상품 ID  
     * @param items 주문 항목 목록
     * @param reservationId 연동할 예약 ID (선택)
     * @return 구매형 주문 엔티티
     * @throws IllegalArgumentException 검증 실패 시
     */
    public static Order createPurchaseOrder(Long customerId, Long storeId, Long productId, List<OrderItem> items, Long reservationId) {
        validatePurchaseItems(items);
        validateBasicFields(customerId, storeId, productId);
        
        int totalAmount = calculateTotalAmount(items);
        
        // 취소 가능 시간: 구매 정책 - 생성 후 1시간  
        LocalDateTime cancelableUntil = LocalDateTime.now().plusHours(1);
        
        return Order.builder()
                .orderNo(generateOrderNo())
                .customerId(customerId)
                .storeId(storeId)
                .productId(productId)
                .orderType(OrderType.PURCHASE)
                .status(OrderStatus.REQUESTED)
                .cancelableUntil(cancelableUntil)
                .totalAmount(totalAmount)
                .items(new ArrayList<>(items))  // 방어적 복사
                .createdAt(LocalDateTime.now())
                .build();
    }
    
    // ========================= 검증 메서드 =========================
    
    /**
     * 기본 필드 검증
     */
    private static void validateBasicFields(Long customerId, Long storeId, Long productId) {
        if (customerId == null) {
            throw new IllegalArgumentException("고객 ID는 필수입니다.");
        }
        if (storeId == null) {
            throw new IllegalArgumentException("스토어 ID는 필수입니다.");
        }
        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }
    }
    
    /**
     * 예약형 주문 항목 검증
     */
    private static void validateReservationItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어있습니다.");
        }
        
        boolean hasNonReservationItem = items.stream()
                .anyMatch(item -> !item.isReservationType());
        
        if (hasNonReservationItem) {
            throw new IllegalArgumentException("예약 주문에는 예약 항목만 포함되어야 합니다.");
        }
    }
    
    /**
     * 구매형 주문 항목 검증  
     */
    private static void validatePurchaseItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 비어있습니다.");
        }
        
        boolean hasNonMerchItem = items.stream()
                .anyMatch(item -> !item.isMerchType());
        
        if (hasNonMerchItem) {
            throw new IllegalArgumentException("구매 주문에는 굿즈 항목만 포함되어야 합니다.");
        }
    }
    
    /**
     * 총 금액 계산
     */
    private static int calculateTotalAmount(List<OrderItem> items) {
        return items.stream()
                .mapToInt(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
    }
    
    // ========================= 비즈니스 로직 메서드 =========================
    
    /**
     * 현재 시점에서 취소 가능한지 확인
     * @return 취소 가능하면 true
     */
    public boolean isCancelable() {
        return cancelableUntil != null && LocalDateTime.now().isBefore(cancelableUntil);
    }
    
    /**
     * 특정 시점에서 취소 가능한지 확인
     * @param checkTime 확인할 시점
     * @return 취소 가능하면 true
     */
    public boolean isCancelableAt(LocalDateTime checkTime) {
        return cancelableUntil != null && checkTime.isBefore(cancelableUntil);
    }
    
    /**
     * 주문 상태 변경
     * @param newStatus 새로운 상태
     * @return 상태가 변경된 주문 (불변성 유지)
     */
    public Order changeStatus(OrderStatus newStatus) {
        return Order.builder()
                .id(this.id)
                .orderNo(this.orderNo)
                .customerId(this.customerId)
                .storeId(this.storeId)
                .productId(this.productId)
                .orderType(this.orderType)
                .status(newStatus)
                .cancelableUntil(this.cancelableUntil)
                .totalAmount(this.totalAmount)
                .items(this.items)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
    
    /**
     * 예약형 주문인지 확인
     * @return 예약형이면 true
     */
    public boolean isReservationType() {
        return OrderType.RESERVATION.equals(orderType);
    }
    
    /**
     * 구매형 주문인지 확인
     * @return 구매형이면 true
     */
    public boolean isPurchaseType() {
        return OrderType.PURCHASE.equals(orderType);
    }
    
    /**
     * 총 주문 항목 수량 계산
     * @return 모든 항목의 수량 합계
     */
    public int getTotalQuantity() {
        return items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }
}
