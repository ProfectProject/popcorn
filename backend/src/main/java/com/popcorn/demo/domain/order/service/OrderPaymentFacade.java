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

		// 🎯 결제 기록은 생성하지 않고, 주문만 생성
		// 실제 결제는 프론트엔드에서 토스 결제 완료 후 webhook으로 처리

		// 🎯 결제 정보 제공용 (실제 결제 기록 없이)
		PaymentCommandService.PaymentCreationResult mockPaymentResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(null) // 아직 결제 기록 없음
						.paymentStatus(null)
						.orderStatus(orderResponse.getStatus() != null ?
								OrderStatus.valueOf(orderResponse.getStatus()) : OrderStatus.REQUESTED)
						.orderNo(orderResponse.getOrderNo())
						.amount(orderResponse.getTotalAmount())
						.customerId(extractCustomerIdFromCommand(command))
						.approvedAt(null)
						.build();

		return OrderWithPaymentResult.builder()
				.orderResponse(orderResponse)
				.paymentResult(mockPaymentResult)
				.build();
	}

	private Long extractCustomerIdFromCommand(CreateOrderCommand command) {
		return command.getUserId();
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
