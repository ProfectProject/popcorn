package com.popcorn.demo.domain.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;
import com.popcorn.demo.global.config.CommonConfig;

/**
 * 결제 기록 생성 API 테스트
 */
@DisplayName("결제 기록 생성 API 테스트")
class PaymentCommandControllerTest {

	private MockMvc mockMvc;
	private ObjectMapper objectMapper;
	private PaymentCommandService paymentCommandService;
	private TossPaymentsProperties tossPaymentsProperties;

	@BeforeEach
	void setUp() {
		paymentCommandService = Mockito.mock(PaymentCommandService.class);
		objectMapper = new CommonConfig().objectMapper();
		tossPaymentsProperties = new TossPaymentsProperties();
		tossPaymentsProperties.setSuccessUrl("http://localhost:3000/payments/success");
		tossPaymentsProperties.setFailUrl("http://localhost:3000/payments/fail");

		PaymentCommandController controller = new PaymentCommandController(
				paymentCommandService, objectMapper, tossPaymentsProperties);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new OrderExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("성공: 예약 결제 기록 생성")
	void createReservationPayment_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004003");
		when(paymentCommandService.createReservationPayment(
				eq(orderId), eq("CARD"), eq(4000), any()))
				.thenReturn(PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.orderNo("O20251231-001003")
						.amount(4000)
						.customerId(1001L)
						.approvedAt(null)
						.build());

		mockMvc.perform(post("/api/v1/orders/{orderId}/reservation-payments", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "method": "CARD",
					  "amount": 4000,
					  "rawPayload": {
					    "pg": "example",
					    "transactionId": "T-20250101"
					  }
					}
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
				.andExpect(jsonPath("$.data.status").value("READY"))
				.andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_PENDING"));
	}

	@Test
	@DisplayName("성공: 구매 결제 기록 생성")
	void createOrderPayment_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001004");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004004");

		when(paymentCommandService.createOrderPayment(
				eq(orderId), eq("CARD"), eq(3000), any()))
				.thenReturn(PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.orderNo("O20251231-001004")
						.amount(3000)
						.customerId(1002L)
						.build());

		mockMvc.perform(post("/api/v1/orders/{orderId}/payments", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "method": "CARD",
					  "amount": 3000,
					  "rawPayload": {
					    "pg": "example",
					    "transactionId": "T-20250102"
					  }
					}
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
				.andExpect(jsonPath("$.data.status").value("READY"))
				.andExpect(jsonPath("$.data.orderStatus").value("PAYMENT_PENDING"));
	}

	@Test
	@DisplayName("성공: READY 결제 기록 생성")
	void createReadyPayment_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001005");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004005");

		when(paymentCommandService.createReadyPayment(
				eq(orderId), eq("CARD"), eq(3000), any()))
				.thenReturn(PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.REQUESTED)
						.build());

		mockMvc.perform(post("/api/v1/orders/{orderId}/payments/ready", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "method": "CARD",
					  "amount": 3000
					}
					"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.paymentId").value(paymentId.toString()))
				.andExpect(jsonPath("$.data.status").value("READY"))
				.andExpect(jsonPath("$.data.orderStatus").value("REQUESTED"));
	}

	@Test
	@DisplayName("실패: 잘못된 결제 수단")
	void createReservationPayment_invalidMethod() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");

		when(paymentCommandService.createReservationPayment(
				eq(orderId), eq("VIRTUAL"), eq(4000), any()))
				.thenThrow(PaymentException.invalidRequest());

		mockMvc.perform(post("/api/v1/orders/{orderId}/reservation-payments", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "method": "VIRTUAL",
					  "amount": 4000
					}
					"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message").value("잘못된 요청입니다."));
	}

	@Test
	@DisplayName("실패: 결제 기록 중복")
	void createReservationPayment_duplicate() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");

		when(paymentCommandService.createReservationPayment(
				eq(orderId), eq("CARD"), eq(4000), any()))
				.thenThrow(PaymentException.paymentAlreadyExists());

		mockMvc.perform(post("/api/v1/orders/{orderId}/reservation-payments", orderId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "method": "CARD",
					  "amount": 4000
					}
					"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(1304))
				.andExpect(jsonPath("$.message").value("해당 주문에 결제 기록이 이미 존재합니다."));
	}
}
