package com.popcorn.demo.domain.order.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**

	* 주문 엔티티 (R2DBC)

	* BaseEntity를 상속받아 표준화된 감사 필드를 포함합니다.

	*

	* 매핑 테이블: p_orders

	*

	* 주요 기능:

	* - 예약형/구매형 주문 통합 관리

	* - 주문 상태 변화와 취소 정책 관리

	* - 주문 번호 자동 생성

	*/

@Table("p_orders")

@Getter

@Setter

@NoArgsConstructor

@AllArgsConstructor

@Builder

public class Order extends BaseEntity {



	// ========================= 기본 필드 =========================



	/** 주문 ID (Primary Key) */

	@Id
	private UUID id;



	/** 주문 번호 (고유 식별자) */

	@Column("order_no")

	private String orderNo;



	/** 고객 ID */

	@Column("customer_id")

	private Long customerId;



	/** 스토어 ID */

	@Column("store_id")

	private UUID storeId;



	/** 상품 ID */

	@Column("product_id")

	private UUID productId;



	/** 주문 타입 (예약형/구매형) */

	@Column("order_type")

	private OrderType orderType;



	/** 주문 상태 */

	@Column("status")

	private OrderStatus status;



	/** 취소 가능 시간 */

	@Column("cancelable_until")

	private LocalDateTime cancelableUntil;



	/** 총 주문 금액 (원 단위) */

	@Column("total_amount")

	private Integer totalAmount;



	/** 멱등성 키 (중복 주문 방지용) */

	@Column("idempotency_key")

	private String idempotencyKey;

	/** 낙관적 락 버전 */

	@Version
	@Column("version")

	private Long version;



	// TODO: 주소 정보는 별도 테이블로 관리하거나 향후 스키마 확장 필요

	// 현재 p_orders 테이블에는 주소 필드가 없음



	/** 주문 항목 목록 */

	@Transient

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



	/**
		* 주문 항목 추가 (리액티브 환경에서는 연관관계를 컬렉션으로만 유지)
		*/

	public void addOrderItem(OrderItem orderItem) {
		if (orderItem == null) {
			return;
		}
		orderItems.add(orderItem);
	}



	/**
		* 주문 항목 목록 추가 (양방향 연관관계 유지)
		*/

	public void addOrderItems(List<OrderItem> items) {
		if (items == null) {
			return;
		}
		items.forEach(this::addOrderItem);
	}

}
