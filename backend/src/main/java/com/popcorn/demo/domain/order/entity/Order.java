package com.popcorn.demo.domain.order.entity;

import com.popcorn.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

/**
 * 주문 엔티티 (JPA)
 * BaseEntity를 상속받아 표준화된 감사 필드를 포함합니다.
 *
 * 매핑 테이블: p_orders
 *
 * 주요 기능:
 * - 예약형/구매형 주문 통합 관리
 * - 주문 상태 변화와 취소 정책 관리
 * - 주문 번호 자동 생성
 */
@Entity
@Table(name = "p_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {
    
    // ========================= 기본 필드 =========================

    /** 주문 ID (Primary Key) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 주문 번호 (고유 식별자) */
    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    /** 고객 ID */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /** 스토어 ID */
    @Column(name = "store_id", nullable = false)
    private Long storeId;

    /** 상품 ID */
    @Column(name = "product_id", nullable = false)
    private Long productId;

    /** 주문 타입 (예약형/구매형) */
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 20)
    private OrderType orderType;

    /** 주문 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    /** 취소 가능 시간 */
    @Column(name = "cancelable_until")
    private LocalDateTime cancelableUntil;

    /** 총 주문 금액 (원 단위) */
    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;

    // TODO: 주소 정보는 별도 테이블로 관리하거나 향후 스키마 확장 필요
    // 현재 p_orders 테이블에는 주소 필드가 없음

    /** 주문 항목 목록 */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    // BaseEntity에서 상속받는 필드들:
    // - createdAt: 생성 시간
    // - updatedAt: 수정 시간
    
    // ========================= 편의 메서드 =========================

    /**
     * 주문 번호 생성
     * 형식: O + YYYYMMDD + 6자리 시퀀스
     * 예: O20251230-000001
     */
    public static String generateOrderNo() {
        String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long sequence = System.currentTimeMillis() % 1000000;
        return "O" + dateStr + "-" + String.format("%06d", sequence);
    }

    /**
     * 예약형 주문인지 확인
     */
    public boolean isReservationType() {
        return OrderType.RESERVATION.equals(orderType);
    }

    /**
     * 구매형 주문인지 확인
     */
    public boolean isPurchaseType() {
        return OrderType.PURCHASE.equals(orderType);
    }

    /**
     * 현재 시점에서 취소 가능한지 확인
     */
    public boolean isCancelable() {
        return cancelableUntil != null && LocalDateTime.now().isBefore(cancelableUntil);
    }

    /**
     * 총 주문 항목 수량 계산
     */
    public int getTotalQuantity() {
        return orderItems.stream()
                .mapToInt(OrderItem::getQty)
                .sum();
    }
}
