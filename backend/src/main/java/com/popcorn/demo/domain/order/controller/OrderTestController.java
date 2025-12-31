package com.popcorn.demo.domain.order.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.application.order.port.in.CreateOrderCommand;
import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.usecase.CreateOrderUseCase;
import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.dto.OrderCreatedDto;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.exception.OrderException;

/**
	* 로컬/개발 환경에서만 사용하는 테스트 주문 생성 API
	*/

@Profile({ "local", "dev" })

@RestController

@RequestMapping("/api/v1/orders/test")

public class OrderTestController extends BaseController {



	private final CreateOrderUseCase createOrderUseCase;



	public OrderTestController(CreateOrderUseCase createOrderUseCase) {

		this.createOrderUseCase = createOrderUseCase;

	}



	@PostMapping("/{userId}")

	public ResponseEntity<BaseResponse<OrderCreatedDto>> createTestOrder(

			@PathVariable Long userId,

			@RequestParam(defaultValue = "RESERVATION") String orderType,

			@RequestParam(defaultValue = "1") Long storeId,

			@RequestParam(defaultValue = "1") Long productId,

			@RequestParam(required = false) Long sessionId,

			@RequestParam(required = false) Long merchVariantId,

			@RequestParam(defaultValue = "1") Integer qty,

			@RequestParam(defaultValue = "1000") Integer unitPrice) {



		OrderItemType itemType = resolveItemType(orderType);

		CreateOrderCommand.OrderItemCommand itemCommand = CreateOrderCommand.OrderItemCommand.builder()

				.orderItemType(itemType)

				.sessionId(sessionId)

				.merchVariantId(merchVariantId)

				.qty(qty)

				.unitPrice(unitPrice)

				.build();



		CreateOrderCommand command = CreateOrderCommand.builder()

				.userId(userId)

				.storeId(storeId)

				.productId(productId)

				.orderType(orderType.toUpperCase())

				.items(List.of(itemCommand))

				.build();



		CreateOrderResponse response = createOrderUseCase.createOrder(command);

		OrderCreatedDto orderCreatedDto = convertToOrderCreatedDto(response);

		BaseResponse<OrderCreatedDto> baseResponse = BaseResponse.success(orderCreatedDto);

		return new ResponseEntity<>(baseResponse, HttpStatus.CREATED);

	}



	private OrderItemType resolveItemType(String orderType) {

		if (orderType == null) {

			throw OrderException.invalidRequest();

		}

		String normalized = orderType.toUpperCase();

		if ("RESERVATION".equals(normalized)) {

			return OrderItemType.RESERVATION;

		}

		if ("PURCHASE".equals(normalized)) {

			return OrderItemType.MERCH;

		}

		throw OrderException.invalidRequest();

	}



	private OrderCreatedDto convertToOrderCreatedDto(CreateOrderResponse response) {

		return new OrderCreatedDto(

				response.getOrderId(),

				response.getOrderNo(),

				response.getOrderType(),

				response.getStatus(),

				response.getStoreId(),

				response.getProductId(),

				response.getTotalAmount(),

				response.getCancelableUntil(),

				response.getCreatedAt(),

				response.getItems().stream()

						.map(item -> new OrderCreatedDto.OrderItemDto(

								item.getItemId(),

								item.getOrderItemType(),

								item.getQty(),

								item.getUnitPrice(),

								item.getLineAmount()

						))

						.toList()

		);

	}

}
