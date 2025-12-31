package com.popcorn.demo.application.order.port.in;

import java.util.List;

import com.popcorn.demo.domain.order.entity.OrderItemType;

import lombok.Builder;
import lombok.Getter;

/**

	* 주문 생성 명령 (Command)

	*

	* Clean Architecture의 Input Port에서 사용되는 명령 객체입니다.

	* - 불변 객체 (Immutable)

	* - 프레젠테이션 계층으로부터 입력받은 데이터

	* - 유효성 검증 완료된 데이터

	*/

@Getter

@Builder

public class CreateOrderCommand {



	private final Long userId;

	private final Long storeId;

	private final Long productId;

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

		private final Long sessionId;

		private final Long merchVariantId;

		private final Integer qty;

		private final Integer unitPrice;

	}

}

