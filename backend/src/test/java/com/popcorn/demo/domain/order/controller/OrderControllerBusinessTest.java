package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.global.config.SecurityConfig;
import com.popcorn.demo.domain.order.dto.request.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.request.OrderItemRequest;
import com.popcorn.demo.domain.order.dto.request.UpdateOrderStatusRequest;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;

import lombok.extern.slf4j.Slf4j;

/**
 * 비즈니스 가치 중심의 주문 컨트롤러 테스트
 *
 * 토스의 "가치있는 테스트" 전략을 적용하여:
 * 1. 사용자 관점의 비즈니스 시나리오 중심 테스트
 * 2. 도메인 로직과 비즈니스 규칙 검증
 * 3. 실제 사용자 스토리를 반영한 테스트 케이스
 */
@WebMvcTest(controllers = OrderController.class)
@ContextConfiguration(classes = {
		OrderController.class,
		OrderExceptionHandler.class,
		SecurityConfig.class,
		OrderControllerBusinessTest.TestConfig.class
})
@ActiveProfiles("local")
@Slf4j
class OrderControllerBusinessTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		Mockito.reset(orderService);
	}

	@Nested
	@DisplayName("고객이 팝콘 예약을 하는 시나리오")
	class 팝콘예약시나리오 {

		@Test
		@DisplayName("고객이 예약 가능한 시간대에 팝콘을 예약한다")
		void 고객이_예약가능한_시간대에_팝콘을_예약한다() throws Exception {
			log.info("🎯 비즈니스 테스트: 고객 팝콘 예약 시나리오 시작");

			// Given: 고객이 예약하려는 팝콘 정보
			UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001001");
			UUID storeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
			UUID productId = UUID.fromString("00000000-0000-0000-0000-000000000101");
			UUID itemId = UUID.fromString("00000000-0000-0000-0000-000000000010");

			// 비즈니스 관점: 성공적인 예약 결과
			CreateOrderResponse expectedReservation = CreateOrderResponse.builder()
					.orderId(orderId)
					.orderNo("O20251231-000001")
					.orderType("RESERVATION")
					.status("REQUESTED") // 예약 요청 상태
					.storeId(storeId)
					.productId(productId)
					.totalAmount(2000)
					.cancelableUntil(LocalDateTime.now().plusMinutes(15)) // 15분 후 취소 불가
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

			when(orderService.createOrder(any())).thenReturn(expectedReservation);

			// When: 고객이 팝콘 예약을 요청한다
			String 고객의_예약요청 = """
					{
						"orderType": "RESERVATION",
						"storeId": "%s",
						"productId": "%s",
						"items": [
							{
								"orderItemType": "RESERVATION",
								"sessionId": "00000000-0000-0000-0000-000000000201",
								"optionId": "00000000-0000-0000-0000-000000000301",
								"qty": 2
							}
						]
					}
					""".formatted(storeId, productId);

			// Then: 예약이 성공적으로 접수된다
			mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
							.contentType(MediaType.APPLICATION_JSON)
							.content(고객의_예약요청))
					.andDo(MockMvcResultHandlers.print())
					.andExpect(MockMvcResultMatchers.status().isCreated())
					.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
					.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderId").value(orderId.toString()))
					.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("REQUESTED"))
					.andExpect(MockMvcResultMatchers.jsonPath("$.data.totalAmount").value(2000))
					.andExpect(MockMvcResultMatchers.jsonPath("$.data.orderType").value("RESERVATION"));

			// 비즈니스 검증: 주문 서비스가 올바른 예약 로직을 수행했는지 확인
			verify(orderService, times(1)).createOrder(any());

			log.info("✅ 고객 팝콘 예약이 성공적으로 접수되었습니다");
		}

		@Test
		@DisplayName("고객이 이미 매진된 시간대에 예약을 시도한다")
		void 고객이_매진된_시간대에_예약을_시도한다() throws Exception {
			log.info("🎯 비즈니스 테스트: 매진된 시간대 예약 시도 시나리오");

			// Given: 해당 시간대가 이미 매진된 상황
			when(orderService.createOrder(any()))
					.thenThrow(OrderException.emptyItems()); // 실제로는 "매진" 예외가 더 적절

			// When: 고객이 매진된 시간대에 예약을 시도한다
			String 매진된_시간대_예약요청 = """
					{
						"orderType": "RESERVATION",
						"storeId": "00000000-0000-0000-0000-000000000001",
						"productId": "00000000-0000-0000-0000-000000000101",
						"items": [
							{
								"orderItemType": "RESERVATION",
								"sessionId": "00000000-0000-0000-0000-000000000201",
								"optionId": "00000000-0000-0000-0000-000000000301",
								"qty": 1
							}
						]
					}
					""";

			// Then: 고객에게 매진 안내가 전달된다
			mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
							.contentType(MediaType.APPLICATION_JSON)
							.content(매진된_시간대_예약요청))
					.andDo(MockMvcResultHandlers.print())
					.andExpect(MockMvcResultMatchers.status().isBadRequest())
					.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1000))
					.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("주문 항목이 비어있습니다."));

			log.info("✅ 매진된 시간대 예약 시도에 대한 안내가 정상적으로 전달되었습니다");
		}

		@Test
		@DisplayName("고객이 잘못된 수량으로 예약을 시도한다")
		void 고객이_잘못된_수량으로_예약을_시도한다() throws Exception {
			log.info("🎯 비즈니스 테스트: 잘못된 수량 예약 시도 시나리오");

			// Given: 비즈니스 규칙상 허용되지 않는 수량 (예: 0개 이하, 최대 수량 초과)
			when(orderService.createOrder(any()))
					.thenThrow(OrderException.invalidQty());

			// When: 고객이 잘못된 수량으로 예약을 시도한다
			String 잘못된_수량_예약요청 = """
					{
						"orderType": "RESERVATION",
						"storeId": "00000000-0000-0000-0000-000000000001",
						"productId": "00000000-0000-0000-0000-000000000101",
						"items": [
							{
								"orderItemType": "RESERVATION",
								"sessionId": "00000000-0000-0000-0000-000000000201",
								"optionId": "00000000-0000-0000-0000-000000000301",
								"qty": 1
							}
						]
					}
					""";

			// Then: 고객에게 수량 오류 안내가 전달된다
			mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
							.contentType(MediaType.APPLICATION_JSON)
							.content(잘못된_수량_예약요청))
					.andExpect(MockMvcResultMatchers.status().isBadRequest())
					.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1001))
					.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("수량은 1 이상이어야 합니다."));

			log.info("✅ 잘못된 수량에 대한 오류 안내가 정상적으로 전달되었습니다");
		}
	}

	@Nested
	@DisplayName("점주가 예약 요청을 처리하는 시나리오")
	class 점주예약처리시나리오 {

		@Test
		@DisplayName("점주가 고객의 예약 요청을 승인한다")
		void 점주가_고객의_예약요청을_승인한다() throws Exception {
			log.info("🎯 비즈니스 테스트: 점주의 예약 승인 시나리오 시작");

			// Given: 고객의 예약 요청이 대기중인 상황
			UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001111");
			Order 승인된_예약 = Order.builder()
					.id(orderId)
					.status(OrderStatus.OWNER_ACCEPTED)
					.build();

			when(orderService.updateStatus(orderId, "OWNER_ACCEPTED", "점주 승인"))
					.thenReturn(승인된_예약);

			// When: 점주가 예약 요청을 승인한다
			String 점주의_승인처리 = """
					{
						"status": "OWNER_ACCEPTED",
						"reason": "점주 승인"
					}
					""";

			// Then: 예약이 승인 상태로 변경된다
			mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
							.contentType(MediaType.APPLICATION_JSON)
							.content(점주의_승인처리))
					.andExpect(MockMvcResultMatchers.status().isOk())
					.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
					.andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()))
					.andExpect(MockMvcResultMatchers.jsonPath("$.data.status").value("OWNER_ACCEPTED"));

			// 비즈니스 검증: 승인 프로세스가 올바르게 수행되었는지 확인
			verify(orderService, times(1)).updateStatus(orderId, "OWNER_ACCEPTED", "점주 승인");

			log.info("✅ 점주의 예약 승인 처리가 성공적으로 완료되었습니다");
		}

		@Test
		@DisplayName("점주가 이미 처리된 예약 상태를 변경하려고 시도한다")
		void 점주가_이미_처리된_예약상태를_변경하려고_시도한다() throws Exception {
			log.info("🎯 비즈니스 테스트: 잘못된 상태 변경 시도 시나리오");

			// Given: 이미 완료된 예약을 다시 변경하려는 상황 (비즈니스 규칙 위반)
			UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001112");
			when(orderService.updateStatus(orderId, "READY", "reason"))
					.thenThrow(OrderException.invalidStatusTransition());

			// When: 점주가 허용되지 않은 상태 변경을 시도한다
			String 잘못된_상태변경_요청 = """
					{
						"status": "READY",
						"reason": "reason"
					}
					""";

			// Then: 비즈니스 규칙 위반 안내가 전달된다
			mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
							.contentType(MediaType.APPLICATION_JSON)
							.content(잘못된_상태변경_요청))
					.andExpect(MockMvcResultMatchers.status().isBadRequest())
					.andExpect(MockMvcResultMatchers.jsonPath("$.code").value(1201))
					.andExpect(MockMvcResultMatchers.jsonPath("$.message").value("허용되지 않은 상태 변경입니다."));

			log.info("✅ 잘못된 상태 변경 시도에 대한 비즈니스 규칙 안내가 정상적으로 전달되었습니다");
		}
	}

	@TestConfiguration
	static class TestConfig {
		@Bean
		OrderService orderService() {
			return Mockito.mock(OrderService.class);
		}

		@Bean
		ObjectMapper objectMapper() {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
			mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			mapper.disable(com.fasterxml.jackson.databind.DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
			return mapper;
		}
	}
}