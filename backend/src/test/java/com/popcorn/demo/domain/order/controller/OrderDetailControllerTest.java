package com.popcorn.demo.domain.order.controller;

import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.dto.response.OrderDetailDto;
import com.popcorn.demo.domain.order.exception.OrderForbiddenException;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.global.config.CommonConfig;

class OrderDetailControllerTest {

	private MockMvc mockMvc;

	private OrderQueryService orderQueryService;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		orderQueryService = Mockito.mock(OrderQueryService.class);
		objectMapper = new CommonConfig().objectMapper();

		// 주문 상세 조회는 Query 작업이므로 OrderQueryController를 사용
		OrderQueryController queryController = new OrderQueryController(orderQueryService);
		mockMvc = MockMvcBuilders.standaloneSetup(queryController)
				.setControllerAdvice(new OrderExceptionHandler())
				.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
				.build();
	}

	@Test
	@DisplayName("주문 상세 조회 성공")
	void getOrderDetail_success() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000010");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000155");
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000002001");

		OrderDetailDto detail = OrderDetailDto.builder()
				.id(orderId)
				.orderNo("O20251231-001001")
				.orderType("RESERVATION")
				.status("REQUESTED")
				.customerId(1L)
				.customer(OrderDetailDto.CustomerDto.builder()
						.id(1L)
						.role("USER")
						.build())
				.storeId(storeId)
				.productId(productId)
				.totalAmount(4000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.items(List.of(
						OrderDetailDto.ItemDto.builder()
								.id(itemId)
								.orderItemType("RESERVATION")
								.productId(productId)
								.productTitle("Seed Popup 55")
								.productCategory("POPUP")
								.productStatus("OPEN")
								.qty(2)
								.unitPrice(2000)
								.lineAmount(4000)
								.build()
				))
				.address(OrderDetailDto.AddressDto.builder()
						.address1("서울특별시 강남구 테헤란로 123")
						.address2("ABC빌딩 12층 1201호")
						.receiverName("Seed User")
						.phone("01000000000")
						.build())
				.payment(OrderDetailDto.PaymentDto.builder()
						.id(UUID.fromString("00000000-0000-0000-0000-000000004001"))
						.method("CARD")
						.status("APPROVED")
						.amount(4000)
						.build())
				.build();

		when(orderQueryService.getOrderDetail(orderId, null, null)).thenReturn(detail);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + orderId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderNo").value("O20251231-001001"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].id").value(itemId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.address.address1").value("서울특별시 강남구 테헤란로 123"));
	}

	@Test
	@DisplayName("주문 상세 조회 - 예약 항목 정보 노출")
	void getOrderDetail_reservationFields() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001010");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000010");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000155");
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000002010");
		UUID sessionId = UUID.fromString("00000000-0000-0000-0000-000000000201");
		UUID optionId = UUID.fromString("00000000-0000-0000-0000-000000000301");
		LocalDateTime sessionStart = LocalDateTime.now().minusDays(1);
		LocalDateTime sessionEnd = LocalDateTime.now().plusDays(1);

		OrderDetailDto detail = OrderDetailDto.builder()
				.id(orderId)
				.orderNo("O20251231-001010")
				.orderType("RESERVATION")
				.status("REQUESTED")
				.customerId(1L)
				.customer(OrderDetailDto.CustomerDto.builder()
						.id(1L)
						.role("USER")
						.build())
				.storeId(storeId)
				.productId(productId)
				.totalAmount(2000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.items(List.of(
						OrderDetailDto.ItemDto.builder()
								.id(itemId)
								.orderItemType("RESERVATION")
								.productId(productId)
								.productTitle("Seed Popup 55")
								.productCategory("POPUP")
								.productStatus("OPEN")
								.sessionId(sessionId)
								.optionId(optionId)
								.sessionStartAt(sessionStart)
								.sessionEndAt(sessionEnd)
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();

		when(orderQueryService.getOrderDetail(orderId, null, null)).thenReturn(detail);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + orderId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].sessionId").value(sessionId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].optionId").value(optionId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].sessionStartAt").exists())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].sessionEndAt").exists());
	}

	@Test
	@DisplayName("주문 상세 조회 - 굿즈 항목 정보 노출")
	void getOrderDetail_merchFields() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001011");
		UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000102");
		UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000002011");
		UUID merchVariantId = UUID.fromString("00000000-0000-0000-0000-000000000401");

		OrderDetailDto detail = OrderDetailDto.builder()
				.id(orderId)
				.orderNo("O20251231-001011")
				.orderType("PURCHASE")
				.status("REQUESTED")
				.customerId(1L)
				.customer(OrderDetailDto.CustomerDto.builder()
						.id(1L)
						.role("USER")
						.build())
				.storeId(storeId)
				.productId(productId)
				.totalAmount(3000)
				.cancelableUntil(LocalDateTime.now())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.items(List.of(
						OrderDetailDto.ItemDto.builder()
								.id(itemId)
								.orderItemType("MERCH")
								.productId(productId)
								.productTitle("Seed Merch 2")
								.productCategory("MERCH")
								.productStatus("OPEN")
								.merchVariantId(merchVariantId)
								.merchVariantName("Seed Merch Variant")
								.merchSku("SEED-SKU-401")
								.qty(2)
								.unitPrice(1500)
								.lineAmount(3000)
								.build()
				))
				.build();

		when(orderQueryService.getOrderDetail(orderId, null, null)).thenReturn(detail);

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + orderId)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].merchVariantId")
						.value(merchVariantId.toString()))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].merchVariantName")
						.value("Seed Merch Variant"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].merchSku")
						.value("SEED-SKU-401"));
	}

	@Test
	@DisplayName("주문 상세 조회 실패 - 주문 없음")
	void getOrderDetail_notFound() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000009999");
		when(orderQueryService.getOrderDetail(orderId, null, null))
				.thenThrow(OrderNotFoundException.orderNotFound());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + orderId))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1100))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("주문을 찾을 수 없습니다."));
	}

	@Test
	@DisplayName("주문 상세 조회 실패 - 권한 없음")
	void getOrderDetail_forbidden() throws Exception {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000009998");
		when(orderQueryService.getOrderDetail(orderId, null, null))
				.thenThrow(OrderForbiddenException.forbidden());

		mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/orders/" + orderId))
				.andExpect(MockMvcResultMatchers.status().isForbidden())
				.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
				.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."));
	}
}
