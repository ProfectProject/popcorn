package com.popcorn.demo.domain.order.entity;

import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
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
	@GeneratedValue
	@UuidGenerator
	private UUID id;



	@Column(name = "order_id")
	private UUID orderId;



	@Enumerated(EnumType.STRING)
	@JdbcType(PostgreSQLEnumJdbcType.class)
	@Column(name = "order_item_type")

	private OrderItemType orderItemType;



	@Column(name = "session_option_id")

	private UUID sessionOptionId;



	@Column(name = "merch_variant_id")

	private UUID merchVariantId;



	@Column(name = "qty")

	private Integer qty;



	@Column(name = "unit_price")

	private Integer unitPrice;



	@Column(name = "line_amount")

	private Integer lineAmount;



	// 편의 메서드

	public boolean isReservationType() {

		return OrderItemType.RESERVATION.equals(orderItemType);

	}



	public boolean isMerchType() {

		return OrderItemType.MERCH.equals(orderItemType);

	}

}
