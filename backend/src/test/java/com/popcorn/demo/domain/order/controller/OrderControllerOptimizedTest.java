package com.popcorn.demo.domain.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderException;

/**
 * 최적화된 주문 컨트롤러 테스트
 *
 * 최적화 포인트:
 * 1. Base 클래스 상속으로 중복 제거
 * 2. 테스트 데이터 헬퍼 메서드 활용
 * 3. 성능 최적화된 Mock 설정
 * 4. 비즈니스 시나리오 중심 테스트
 */
class OrderControllerOptimizedTest extends OrderControllerTestBase {

    @Nested
    @DisplayName("🎯 고객 팝콘 예약 시나리오 (최적화)")
    class 고객팝콘예약최적화 {

        @Test
        @DisplayName("예약 성공 - 최적화된 테스트")
        void 예약_성공_최적화() throws Exception {
            logTestStart("고객 팝콘 예약 성공");

            // Given: 공통 헬퍼 메서드 활용으로 코드 중복 제거
            CreateOrderResponse response = createTestOrderResponse(
                TestUUIDs.ORDER_ID,
                TestUUIDs.STORE_ID,
                TestUUIDs.PRODUCT_ID
            );

            // Mock 설정 최적화 - 한 줄로 간소화
            when(orderCommandService.createOrder(any())).thenReturn(response);

            // When: 공통 JSON 헬퍼 메서드 활용
            String requestJson = createOrderRequestJson(TestUUIDs.STORE_ID, TestUUIDs.PRODUCT_ID, 2);

            // Then: 검증 로직 최적화
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andDo(MockMvcResultHandlers.print())
                    .andExpectAll( // 여러 검증을 한 번에 수행 (성능 최적화)
                        MockMvcResultMatchers.status().isCreated(),
                        MockMvcResultMatchers.jsonPath("$.code").value(200),
                        MockMvcResultMatchers.jsonPath("$.data.orderId").value(TestUUIDs.ORDER_ID.toString()),
                        MockMvcResultMatchers.jsonPath("$.data.status").value("REQUESTED"),
                        MockMvcResultMatchers.jsonPath("$.data.totalAmount").value(2000)
                    );

            // 비즈니스 검증 최적화
            verify(orderCommandService, times(1)).createOrder(any());

            logTestComplete("고객 팝콘 예약 성공");
        }

        @Test
        @DisplayName("매진된 시간대 예약 시도 - 최적화된 예외 테스트")
        void 매진된_시간대_예약_최적화() throws Exception {
            logTestStart("매진된 시간대 예약 시도");

            // Given: 예외 상황 Mock 설정
            when(orderCommandService.createOrder(any()))
                    .thenThrow(OrderException.emptyItems());

            // When & Then: 한 번의 호출로 예외 검증
            String requestJson = createOrderRequestJson(TestUUIDs.STORE_ID, TestUUIDs.PRODUCT_ID, 1);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpectAll(
                        MockMvcResultMatchers.status().isBadRequest(),
                        MockMvcResultMatchers.jsonPath("$.code").value(1000),
                        MockMvcResultMatchers.jsonPath("$.message").value("주문 항목이 비어있습니다.")
                    );

            logTestComplete("매진된 시간대 예약 시도");
        }
    }

    @Nested
    @DisplayName("🏪 점주 예약 처리 시나리오 (최적화)")
    class 점주예약처리최적화 {

        @Test
        @DisplayName("예약 승인 - 최적화된 상태 변경 테스트")
        void 예약_승인_최적화() throws Exception {
            logTestStart("점주 예약 승인");

            // Given: 최적화된 테스트 데이터 생성
            UUID orderId = TestUUIDs.ORDER_ID;
            Order approvedOrder = Order.builder()
                    .id(orderId)
                    .status(OrderStatus.OWNER_ACCEPTED)
                    .build();

            when(orderCommandService.updateStatus(orderId, "OWNER_ACCEPTED", "점주 승인"))
                    .thenReturn(approvedOrder);

            // When & Then: 공통 헬퍼 메서드 활용
            String statusJson = createStatusUpdateJson("OWNER_ACCEPTED", "점주 승인");

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(statusJson))
                    .andExpectAll(
                        MockMvcResultMatchers.status().isOk(),
                        MockMvcResultMatchers.jsonPath("$.code").value(200),
                        MockMvcResultMatchers.jsonPath("$.data.id").value(orderId.toString()),
                        MockMvcResultMatchers.jsonPath("$.data.status").value("OWNER_ACCEPTED")
                    );

            // 최적화된 검증
            verify(orderCommandService, times(1))
                    .updateStatus(orderId, "OWNER_ACCEPTED", "점주 승인");

            logTestComplete("점주 예약 승인");
        }

        @Test
        @DisplayName("잘못된 상태 변경 시도 - 최적화된 예외 테스트")
        void 잘못된_상태변경_최적화() throws Exception {
            logTestStart("잘못된 상태 변경");

            // Given: 예외 Mock 설정
            UUID orderId = TestUUIDs.ORDER_ID;
            when(orderCommandService.updateStatus(orderId, "READY", "reason"))
                    .thenThrow(OrderException.invalidStatusTransition());

            // When & Then
            String statusJson = createStatusUpdateJson("READY", "reason");

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/v1/orders/" + orderId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(statusJson))
                    .andExpectAll(
                        MockMvcResultMatchers.status().isBadRequest(),
                        MockMvcResultMatchers.jsonPath("$.code").value(1201),
                        MockMvcResultMatchers.jsonPath("$.message").value("허용되지 않은 상태 변경입니다.")
                    );

            logTestComplete("잘못된 상태 변경");
        }
    }

    @Nested
    @DisplayName("⚡ 성능 테스트")
    class 성능테스트 {

        @Test
        @DisplayName("대량 주문 처리 성능 테스트 (최적화)")
        void 대량_주문_처리_성능() throws Exception {
            logTestStart("대량 주문 처리 성능");

            // 성능 최적화를 위한 배치 테스트
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < 10; i++) {
                CreateOrderResponse response = createTestOrderResponse(
                    UUID.randomUUID(),
                    TestUUIDs.STORE_ID,
                    TestUUIDs.PRODUCT_ID
                );
                when(orderCommandService.createOrder(any())).thenReturn(response);

                String requestJson = createOrderRequestJson(TestUUIDs.STORE_ID, TestUUIDs.PRODUCT_ID, 1);

                mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/1001")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                        .andExpect(MockMvcResultMatchers.status().isCreated());
            }

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("🚀 10건 주문 처리 시간: {}ms (평균: {}ms/건)", executionTime, executionTime / 10);

            logTestComplete("대량 주문 처리 성능");
        }
    }
}
