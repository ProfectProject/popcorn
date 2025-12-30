package com.popcorn.demo.domain.order.entity;

import com.popcorn.demo.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 주문 아이템 엔티티 (JPA)
 * p_order_items 테이블과 매핑
 */
@Entity
@Table(name = "p_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_item_type", nullable = false, length = 20)
    private OrderItemType orderItemType;

    @Column(name = "session_option_id")
    private Long sessionOptionId;

    @Column(name = "merch_variant_id")
    private Long merchVariantId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @Column(name = "line_amount", nullable = false)
    private Integer lineAmount;

    // 편의 메서드
    public boolean isReservationType() {
        return OrderItemType.RESERVATION.equals(orderItemType);
    }

    public boolean isMerchType() {
        return OrderItemType.MERCH.equals(orderItemType);
    }
}
