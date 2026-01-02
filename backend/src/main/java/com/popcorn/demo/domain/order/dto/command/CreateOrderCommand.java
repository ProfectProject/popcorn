package com.popcorn.demo.domain.order.dto.command;

import java.util.List;
import java.util.UUID;

import com.popcorn.demo.domain.order.entity.OrderItemType;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 생성 명령 DTO
 * - 서비스 계층으로 전달되는 입력 값
 * - 불변 객체로 사용합니다.
 */

@Getter

@Builder

public class CreateOrderCommand {



	private final Long userId;

	private final UUID storeId;

	private final UUID productId;

	private final String orderType;

	private final String idempotencyKey;

	private final List<OrderItemCommand> items;



	/**

		* 주문 항목 명령

		*/

	@Getter

	@Builder

	public static class OrderItemCommand {

		private final OrderItemType orderItemType;

		private final UUID sessionId;

		private final UUID optionId;

		private final UUID merchVariantId;

		private final Integer qty;

		private final Integer unitPrice;

	}

}
