package com.popcorn.demo.domain.order.entity;

import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.UuidGenerator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**

	* 주문 아이템 엔티티 (JPA)

	* "order".order_goods 테이블과 매핑

	*/

@Entity
@Table(name = "order_goods", schema = "\"order\"")

@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class OrderItem extends BaseEntity {



	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "order_goods_id")
	private UUID id;



	@Column(name = "order_id")
	private UUID orderId;



	@Transient
	private OrderItemType orderItemType;



	@Column(name = "schedule_id")

	private UUID sessionOptionId;



	@Column(name = "goods_variant_id")

	private UUID goodsVariantId;



	@Column(name = "qty")

	private Integer qty;



	@Column(name = "unit_price")

	private Integer unitPrice;



	@Column(name = "price")

	private Integer lineAmount;



	// 편의 메서드

	public boolean isReservationType() {

		return OrderItemType.RESERVATION.equals(orderItemType);

	}



	public boolean isGoodsType() {

		return OrderItemType.GOODS.equals(orderItemType);

	}

}
