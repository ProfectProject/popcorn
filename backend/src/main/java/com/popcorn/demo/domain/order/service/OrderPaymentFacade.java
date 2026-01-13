package com.popcorn.demo.domain.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;

import lombok.RequiredArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Service
@RequiredArgsConstructor
public class OrderPaymentFacade {

	private final OrderCommandService orderCommandService;
	private final PaymentCommandService paymentCommandService;

	@Transactional(transactionManager = "jdbcTransactionManager")
	public OrderWithPaymentResult createOrderWithPayment(CreateOrderCommand command, String paymentMethod) {
		CreateOrderResponse orderResponse = orderCommandService.createOrder(command);
		String method = normalizePaymentMethod(paymentMethod);
		PaymentCommandService.PaymentCreationResult paymentResult =
				paymentCommandService.createPayment(
						orderResponse.getOrderId(),
						method,
						orderResponse.getTotalAmount(),
						null);
		OrderStatus orderStatus = paymentResult.getOrderStatus();
		CreateOrderResponse updatedOrder = updateOrderStatusInResponse(orderResponse, orderStatus);
		return OrderWithPaymentResult.builder()
				.orderResponse(updatedOrder)
				.paymentResult(paymentResult)
				.build();
	}

	private String normalizePaymentMethod(String paymentMethod) {
		if (paymentMethod == null || paymentMethod.isBlank()) {
			return "CARD";
		}
		return paymentMethod.trim();
	}

	private CreateOrderResponse updateOrderStatusInResponse(CreateOrderResponse response, OrderStatus status) {
		if (status == null || status.name().equals(response.getStatus())) {
			return response;
		}
		return CreateOrderResponse.builder()
				.orderId(response.getOrderId())
				.orderNo(response.getOrderNo())
				.orderType(response.getOrderType())
				.status(status.name())
				.storeId(response.getStoreId())
				.popupId(response.getPopupId())
				.totalAmount(response.getTotalAmount())
				.cancelableUntil(response.getCancelableUntil())
				.createdAt(response.getCreatedAt())
				.items(response.getItems())
				.build();
	}

	@Getter
	@Builder
	public static class OrderWithPaymentResult {
		private CreateOrderResponse orderResponse;
		private PaymentCommandService.PaymentCreationResult paymentResult;
	}
}
