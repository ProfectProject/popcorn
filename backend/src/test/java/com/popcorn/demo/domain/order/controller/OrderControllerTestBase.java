package com.popcorn.demo.domain.order.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.service.OrderService;
import com.popcorn.demo.global.config.CommonConfig;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 주문 컨트롤러 테스트 공통 Base 클래스
 *
 * 최적화 포인트:
 * 1. MockMvc 설정 중복 제거
 * 2. ObjectMapper 생성 로직 통합
 * 3. 공통 테스트 헬퍼 메서드 제공
 * 4. 테스트 데이터 생성 로직 중앙화
 */
public abstract class OrderControllerTestBase {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper;
    protected OrderService orderService;

    @BeforeEach
    void setUpBase() {
        // 공통 설정을 한 번만 수행하여 성능 최적화
        orderService = Mockito.mock(OrderService.class);
        objectMapper = createOptimizedObjectMapper();
        mockMvc = createOptimizedMockMvc();

        // 각 테스트 간 격리를 위한 Mock 초기화
        Mockito.reset(orderService);
    }

    /**
     * 최적화된 ObjectMapper 생성 (한 번만 설정)
     */
    private ObjectMapper createOptimizedObjectMapper() {
        return new CommonConfig().objectMapper();
    }

    /**
     * 최적화된 MockMvc 생성 (재사용 가능한 설정)
     */
    private MockMvc createOptimizedMockMvc() {
        return MockMvcBuilders.standaloneSetup(new OrderCommandController(orderService, objectMapper))
                .setControllerAdvice(new OrderExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .alwaysDo(result -> log.debug("테스트 실행 결과: {}",
                    result.getResponse().getContentAsString()))
                .build();
    }

    // ================ 공통 테스트 데이터 헬퍼 메서드들 ================

    /**
     * 테스트용 주문 응답 생성 헬퍼 (재사용 가능)
     */
    protected CreateOrderResponse createTestOrderResponse(UUID orderId, UUID storeId, UUID productId) {
        return CreateOrderResponse.builder()
                .orderId(orderId)
                .orderNo("O" + System.currentTimeMillis())
                .orderType("RESERVATION")
                .status("REQUESTED")
                .storeId(storeId)
                .productId(productId)
                .totalAmount(2000)
                .cancelableUntil(LocalDateTime.now().plusMinutes(15))
                .createdAt(LocalDateTime.now())
                .items(List.of(
                        CreateOrderResponse.OrderItemResponse.builder()
                                .itemId(UUID.randomUUID())
                                .orderItemType(OrderItemType.RESERVATION.name())
                                .qty(2)
                                .unitPrice(1000)
                                .lineAmount(2000)
                                .build()
                ))
                .build();
    }

    /**
     * 테스트용 주문 생성 JSON 헬퍼 (재사용 가능)
     */
    protected String createOrderRequestJson(UUID storeId, UUID productId, int qty) {
        return """
                {
                    "orderType": "RESERVATION",
                    "storeId": "%s",
                    "productId": "%s",
                    "items": [
                        {
                            "orderItemType": "RESERVATION",
                            "sessionId": "00000000-0000-0000-0000-000000000201",
                            "optionId": "00000000-0000-0000-0000-000000000301",
                            "qty": %d
                        }
                    ]
                }
                """.formatted(storeId, productId, qty);
    }

    /**
     * 테스트용 상태 변경 JSON 헬퍼 (재사용 가능)
     */
    protected String createStatusUpdateJson(String status, String reason) {
        return """
                {
                    "status": "%s",
                    "reason": "%s"
                }
                """.formatted(status, reason);
    }

    /**
     * 테스트용 UUID 생성 헬퍼 (일관된 테스트 데이터)
     */
    protected static class TestUUIDs {
        public static final UUID ORDER_ID = UUID.fromString("00000000-0000-0000-0000-000000001001");
        public static final UUID STORE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
        public static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
        public static final UUID ITEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    }

    /**
     * 성능 최적화를 위한 로깅 헬퍼
     */
    protected void logTestStart(String testName) {
        log.info("🧪 테스트 시작: {}", testName);
    }

    protected void logTestComplete(String testName) {
        log.info("✅ 테스트 완료: {}", testName);
    }
}