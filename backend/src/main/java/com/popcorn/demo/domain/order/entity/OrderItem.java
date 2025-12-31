package com.popcorn.demo.domain.order.entity;

import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**

	* 주문 아이템 엔티티 (R2DBC)

	* p_order_items 테이블과 매핑

	*/

@Table("p_order_items")

@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class OrderItem extends BaseEntity {



	@Id
	private UUID id;



	@Column("order_id")
	private UUID orderId;



	@Column("order_item_type")

	private OrderItemType orderItemType;



	@Column("session_option_id")

	private UUID sessionOptionId;



	@Column("merch_variant_id")

	private UUID merchVariantId;



	@Column("qty")

	private Integer qty;



	@Column("unit_price")

	private Integer unitPrice;



	@Column("line_amount")

	private Integer lineAmount;



	// 편의 메서드

	public boolean isReservationType() {

		return OrderItemType.RESERVATION.equals(orderItemType);

	}



	public boolean isMerchType() {

		return OrderItemType.MERCH.equals(orderItemType);

	}

}
