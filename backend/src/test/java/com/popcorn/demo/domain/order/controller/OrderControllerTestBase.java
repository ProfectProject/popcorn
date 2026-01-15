package com.popcorn.demo.domain.order.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import jakarta.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.auth.dto.CustomUserDetails;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.OrderDomainService;
import com.popcorn.demo.domain.order.service.OrderPaymentFacade;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.users.entity.User;
import com.popcorn.demo.domain.users.entity.enums.UserRole;
import com.popcorn.demo.common.config.CommonConfig;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
import com.popcorn.demo.domain.payment.service.PaymentTokenService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;
import com.popcorn.demo.domain.users.repository.UserAddressRepository;

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
    protected OrderCommandService orderCommandService;
    protected OrderDomainService orderDomainService;
    protected OrderRepository orderRepository;
    protected OrderPaymentFacade orderPaymentFacade;
    protected OrderQueryService orderQueryService;
    protected PaymentTokenService paymentTokenService;
    protected UserAddressRepository userAddressRepository;

    @BeforeEach
    void setUpBase() {
        // 공통 설정을 한 번만 수행하여 성능 최적화
        orderCommandService = Mockito.mock(OrderCommandService.class);
        orderDomainService = Mockito.mock(OrderDomainService.class);
        orderRepository = Mockito.mock(OrderRepository.class);
        orderPaymentFacade = Mockito.mock(OrderPaymentFacade.class);
        orderQueryService = Mockito.mock(OrderQueryService.class);
        paymentTokenService = Mockito.mock(PaymentTokenService.class);
        userAddressRepository = Mockito.mock(UserAddressRepository.class);
        objectMapper = createOptimizedObjectMapper();
        mockMvc = createOptimizedMockMvc();

        // 각 테스트 간 격리를 위한 Mock 초기화
        Mockito.reset(orderCommandService, orderDomainService, orderRepository, orderPaymentFacade, orderQueryService, paymentTokenService, userAddressRepository);
    }

    /**
     * 최적화된 ObjectMapper 생성 (한 번만 설정)
     */
    private ObjectMapper createOptimizedObjectMapper() {
        return new CommonConfig().objectMapper();
    }

    /**
     * 최적화된 MockMvc 생성 (CQRS 컨트롤러 지원)
     * Command와 Query 컨트롤러를 모두 설정하여 테스트 가능
     */
    private MockMvc createOptimizedMockMvc() {
        TossPaymentsProperties tossPaymentsProperties = new TossPaymentsProperties();
        tossPaymentsProperties.setSuccessUrl("http://localhost:3000/payments/success");
        tossPaymentsProperties.setFailUrl("http://localhost:3000/payments/fail");
        OrderCommandController commandController = new OrderCommandController(
                orderCommandService, orderDomainService, orderRepository, orderPaymentFacade, userAddressRepository, objectMapper, tossPaymentsProperties, paymentTokenService);
        OrderQueryController queryController = new OrderQueryController(orderQueryService);

        return MockMvcBuilders.standaloneSetup(commandController, queryController)
                .setControllerAdvice(new OrderExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setCustomArgumentResolvers(new AuthenticationArgumentResolver())
                .alwaysDo(result -> log.debug("테스트 실행 결과: {}",
                    result.getResponse().getContentAsString()))
                .build();
    }

    /**
     * Authentication 파라미터 주입을 위한 커스텀 Argument Resolver
     */
    private static class AuthenticationArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(Authentication.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            // MockHttpServletRequest에서 principal을 가져와서 Authentication 객체 반환
            HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
            if (request != null && request.getUserPrincipal() instanceof Authentication) {
                return request.getUserPrincipal();
            }

            // principal이 CustomUserDetails인 경우 Authentication 객체로 래핑
            Object principal = request != null ? request.getUserPrincipal() : null;
            if (principal instanceof CustomUserDetails) {
                return new UsernamePasswordAuthenticationToken(
                    principal, null, ((CustomUserDetails) principal).getAuthorities());
            }

            return null;
        }
    }

    // ================ 공통 테스트 데이터 헬퍼 메서드들 ================

    /**
     * 테스트용 주문 응답 생성 헬퍼 (재사용 가능)
     */
    protected CreateOrderResponse createTestOrderResponse(UUID orderId, UUID storeId, UUID popupId) {
        return CreateOrderResponse.builder()
                .orderId(orderId)
                .orderNo("O" + System.currentTimeMillis())
                .orderType("RESERVATION")
                .status("PAYMENT_PENDING")
                .storeId(storeId)
                .popupId(popupId)
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

    protected OrderPaymentFacade.OrderWithPaymentResult createTestOrderWithPaymentResponse(
            UUID orderId, UUID storeId, UUID popupId) {
        CreateOrderResponse orderResponse = createTestOrderResponse(orderId, storeId, popupId);
        PaymentCommandService.PaymentCreationResult paymentResult =
                PaymentCommandService.PaymentCreationResult.builder()
                        .paymentId(UUID.randomUUID())
                        .amount(orderResponse.getTotalAmount())
                        .customerId(1001L)
                        .build();
        return OrderPaymentFacade.OrderWithPaymentResult.builder()
                .orderResponse(orderResponse)
                .paymentResult(paymentResult)
                .build();
    }

    /**
     * 테스트용 주문 생성 JSON 헬퍼 (재사용 가능)
     */
    protected String createOrderRequestJson(UUID popupId, int qty) {
        return """
                {
                    "orderType": "RESERVATION",
                    "popupId": "%s",
                    "items": [
                        {
                            "orderItemType": "RESERVATION",
                            "sessionId": "00000000-0000-0000-0000-000000000201",
                            "optionId": "00000000-0000-0000-0000-000000000301",
                            "qty": %d
                        }
                    ]
                }
                """.formatted(popupId, qty);
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

    /**
     * 테스트용 Authentication 객체 생성 헬퍼
     */
    protected Authentication createTestAuthentication(Long userId, String email, UserRole role) {
        User testUser = new User();
        testUser.setUserId(userId);
        testUser.setEmail(email);
        testUser.setPassword("password");
        testUser.setRole(role);
        testUser.setName("Test User");
        testUser.setActive(true);

        CustomUserDetails customUserDetails = new CustomUserDetails(testUser);
        return new UsernamePasswordAuthenticationToken(
                customUserDetails,
                null,
                customUserDetails.getAuthorities()
        );
    }

    /**
     * 기본 고객 Authentication 객체 생성 헬퍼
     */
    protected Authentication createCustomerAuthentication() {
        return createTestAuthentication(1001L, "testuser@example.com", UserRole.CUSTOMER);
    }
}
