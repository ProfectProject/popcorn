package com.popcorn.demo.domain.payment.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.controller.OrderExceptionHandler;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.service.TossPaymentService;
import com.popcorn.demo.global.config.CommonConfig;

@DisplayName("토스 결제 승인 API 테스트")
class TossPaymentControllerTest {

	private MockMvc mockMvc;
	private TossPaymentService tossPaymentService;

	@BeforeEach
	void setUp() {
		tossPaymentService = Mockito.mock(TossPaymentService.class);
		ObjectMapper objectMapper = new CommonConfig().objectMapper();

		TossPaymentController controller = new TossPaymentController(tossPaymentService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new OrderExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("성공: 토스 결제 승인")
	void confirmPayment_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004003");
		String orderNo = "O20251231-001003";

		when(tossPaymentService.confirmPayment(
				eq("pay_123"), eq(orderNo), eq(4000)))
				.thenReturn(TossPaymentService.TossPaymentConfirmResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.PAID)
						.orderStatus(OrderStatus.PAID)
						.orderId(orderId)
						.orderNo(orderNo)
						.amount(4000)
						.approvedAt(LocalDateTime.of(2025, 1, 1, 10, 0))
						.build());

		mockMvc.perform(post("/api/v1/payments/toss/confirm")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "paymentKey": "pay_123",
					  "orderId": "O20251231-001003",
					  "amount": 4000
					}
					"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
				.andExpect(jsonPath("$.data.status").value("PAID"))
				.andExpect(jsonPath("$.data.orderStatus").value("PAID"))
				.andExpect(jsonPath("$.data.orderId").value(orderId.toString()))
				.andExpect(jsonPath("$.data.orderNo").value(orderNo))
				.andExpect(jsonPath("$.data.amount").value(4000));
	}
}
