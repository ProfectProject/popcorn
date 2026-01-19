package com.popcorn.demo.domain.order.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.popcorn.demo.common.controller.BaseController;
import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.dto.response.OrderCreatedDto;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.service.OrderService;

@Profile({ "local", "dev" })
@RestController
@RequestMapping("/api/v1/orders/test")
public class OrderTestController extends BaseController {

	private final OrderService orderService;

	public OrderTestController(OrderService orderService) {
		this.orderService = orderService;
	}

	@PostMapping("/{userId}")
	public ResponseEntity<BaseResponse<OrderCreatedDto>> createTestOrder(
			@PathVariable Long userId,
			@RequestParam(defaultValue = "RESERVATION") String orderType,
			@RequestParam(defaultValue = "00000000-0000-0000-0000-000000000101") UUID popupId,
			@RequestParam(required = false) UUID sessionId,
			@RequestParam(required = false) UUID optionId,
			@RequestParam(required = false) UUID goodsVariantId,
			@RequestParam(defaultValue = "1") Integer qty,
			@RequestParam(defaultValue = "1000") Integer unitPrice) {
		OrderItemType itemType = resolveItemType(orderType);
		CreateOrderCommand.OrderItemCommand itemCommand = CreateOrderCommand.OrderItemCommand.builder()
				.orderItemType(itemType)
				.sessionId(sessionId)
				.optionId(optionId)
				.goodsVariantId(goodsVariantId)
				.qty(qty)
				.unitPrice(unitPrice)
				.build();

		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(userId)
				.popupId(popupId)
				.orderType(orderType.toUpperCase())
				.items(List.of(itemCommand))
				.build();

		CreateOrderResponse response = orderService.createOrder(command);
		OrderCreatedDto dto = convertToOrderCreatedDto(response);
		BaseResponse<OrderCreatedDto> baseResponse = BaseResponse.success(dto);
		return new ResponseEntity<>(baseResponse, HttpStatus.CREATED);
	}

	private OrderItemType resolveItemType(String orderType) {
		if (orderType == null) {
			throw OrderValidationException.invalidRequest();
		}

		String normalized = orderType.toUpperCase();
		if ("RESERVATION".equals(normalized)) {
			return OrderItemType.RESERVATION;
		}
		if ("PURCHASE".equals(normalized)) {
			return OrderItemType.GOODS;
		}

		throw OrderValidationException.invalidRequest();
	}

	private OrderCreatedDto convertToOrderCreatedDto(CreateOrderResponse response) {
		return new OrderCreatedDto(
				response.getOrderId(),
				response.getOrderNo(),
				response.getOrderType(),
				response.getStatus(),
				response.getStoreId(),
				response.getPopupId(),
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
						.toList(),
				null,
				null,
				null,
				null,
				null,
				null
		);
	}
}
