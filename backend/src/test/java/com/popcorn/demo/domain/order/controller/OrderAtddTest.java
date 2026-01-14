package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.service.OrderPaymentFacade;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;

class OrderAtddTest extends OrderControllerTestBase {

	@Test
	@DisplayName("ATDD - 주문 생성 후 상태 변경 시나리오")
	void createOrder_then_updateStatus() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000002001");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID popupId = UUID.fromString("00000000-0000-0000-0000-000000000101");
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000000020");

		CreateOrderResponse response = CreateOrderResponse.builder()
				.orderId(orderId)
				.orderNo("O20260101-000001")
				.orderType("RESERVATION")
				.status("PAYMENT_PENDING")
				.storeId(storeId)
				.popupId(popupId)
				.totalAmount(2000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.items(List.of(
						CreateOrderResponse.OrderItemResponse.builder()
								.itemId(itemId)
								.orderItemType(OrderItemType.RESERVATION.name())
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();

		when(orderPaymentFacade.createOrderWithPayment(any(), any()))
				.thenReturn(OrderPaymentFacade.OrderWithPaymentResult.builder()
						.orderResponse(response)
						.paymentResult(PaymentCommandService.PaymentCreationResult.builder()
								.paymentId(UUID.randomUUID())
								.amount(response.getTotalAmount())
								.customerId(1001L)
								.build())
						.build());

		Order updatedOrder = Order.builder()
				.id(orderId)
				.status(OrderStatus.ACCEPTED)
				.build();
		when(orderCommandService.updateStatus(orderId, "ACCEPTED", "approved"))
				.thenReturn(updatedOrder);

		String createJsonRequest = """
				{
					"orderType": "RESERVATION",
					"popupId": "%s",
					"items": [
						{
							"orderItemType": "RESERVATION",
							"sessionId": "00000000-0000-0000-0000-000000000201",
							"optionId": "00000000-0000-0000-0000-000000000301",
							"qty": 2
						}
					]
				}
				""".formatted(popupId);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createJsonRequest)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(itemId.toString()));

		UpdateOrderStatusRequest statusRequest = UpdateOrderStatusRequest.builder()
				.status("ACCEPTED")
				.reason("approved")
				.build();
		String statusBody = objectMapper.writeValueAsString(statusRequest);

		mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(statusBody)
						.principal(createCustomerAuthentication()))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("ACCEPTED"));
	}
}
