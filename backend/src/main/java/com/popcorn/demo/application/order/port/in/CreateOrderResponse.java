package com.popcorn.demo.application.order.port.in;

import java.time.LocalDateTime;
import java.util.List;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 생성 응답 (Response)
 *
 * Clean Architecture의 Input Port에서 반환되는 응답 객체입니다.
 * - 불변 객체 (Immutable)
 * - 도메인 객체로부터 생성
 * - API 계층에서 사용할 수 있는 형태
 */
@Getter
@Builder
public class CreateOrderResponse {

	private final Long orderId;
	private final String orderNo;
	private final String orderType;
	private final String status;
	private final Long storeId;
	private final Long productId;
	private final Integer totalAmount;
	private final LocalDateTime cancelableUntil;
	private final LocalDateTime createdAt;
	private final List<OrderItemResponse> items;

	/**
	 * 주문 항목 응답
	 */
	@Getter
	@Builder
	public static class OrderItemResponse {
		private final Long itemId;
		private final String orderItemType;
		private final Integer qty;
		private final Integer unitPrice;
		private final Integer lineAmount;
	}

	/**
	 * 도메인 엔티티로부터 응답 객체를 생성합니다.
	 */
	public static CreateOrderResponse fromOrder(Order order) {
		List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
				.map(CreateOrderResponse::fromOrderItem)
				.toList();

		return CreateOrderResponse.builder()
				.orderId(order.getId())
				.orderNo(order.getOrderNo())
				.orderType(order.getOrderType().name())
				.status(order.getStatus().name())
				.storeId(order.getStoreId())
				.productId(order.getProductId())
				.totalAmount(order.getTotalAmount())
				.cancelableUntil(order.getCancelableUntil())
				.createdAt(order.getCreatedAt())
				.items(itemResponses)
				.build();
	}

	/**
	 * 주문 항목 엔티티로부터 응답 객체를 생성합니다.
	 */
	private static OrderItemResponse fromOrderItem(OrderItem item) {
		return OrderItemResponse.builder()
				.itemId(item.getId())
				.orderItemType(item.getOrderItemType().name())
				.qty(item.getQty())
				.unitPrice(item.getUnitPrice())
				.lineAmount(item.getLineAmount())
				.build();
	}
}