package com.popcorn.demo.domain.order.entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.popcorn.demo.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
	@GeneratedValue
	@UuidGenerator
	@Column(name = "order_id")
	private UUID id;



	/** 주문 번호 (고유 식별자) */

	@Column(name = "order_no")

	private String orderNo;



	/** 고객 ID */

	@Column(name = "user_id")

	private Long customerId;



	/** 스토어 ID */

	@Column(name = "store_id")

	private UUID storeId;



	/** 팝업 ID (요청값 보관용, 저장되지 않음) */

	@Transient

	private UUID popupId;



	/** 주문 타입 (요청값 보관용, 저장되지 않음) */
	@Transient
	private OrderType orderType;

	/** 주문 상태 */
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private OrderStatus status;



	/** 취소 가능 시간 */

	@Column(name = "cancelable_until")

	private LocalDateTime cancelableUntil;



	/** 총 주문 금액 (원 단위) */

	@Column(name = "total_price")

	private Integer totalAmount;



	/** 멱등성 키 (요청값 보관용, 저장되지 않음) */

	@Transient

	private String idempotencyKey;



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

	/**
	 * 주문 상태 변경 이력 생성
	 * @param reason 변경 사유
	 * @return 주문 상태 이력 객체
	 */
	public OrderStatusHistory toHistory(String reason) {
		return OrderStatusHistory.builder()
				.orderId(this.id)
				.fromStatus(this.status)  // 현재 상태를 fromStatus로 (변경 전 상태)
				.toStatus(this.status)    // 현재 상태를 toStatus로 (변경 후 상태)
				.reason(reason)
				.changedAt(LocalDateTime.now())
				.build();
	}

	/**
	 * 주문 상태 변경 이력 생성 (이전 상태 명시)
	 * @param fromStatus 변경 전 상태
	 * @param reason 변경 사유
	 * @return 주문 상태 이력 객체
	 */
	public OrderStatusHistory toHistory(OrderStatus fromStatus, String reason) {
		return OrderStatusHistory.builder()
				.orderId(this.id)
				.fromStatus(fromStatus)   // 변경 전 상태
				.toStatus(this.status)    // 현재 상태 (변경 후 상태)
				.reason(reason)
				.changedAt(LocalDateTime.now())
				.build();
	}

}
