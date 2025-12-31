package com.popcorn.demo.domain.order.entity;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

	@SequenceGenerator(
			name = "order_item_seq",
			sequenceName = "p_order_items_id_seq",
			allocationSize = 1
	)

	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_item_seq")

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
