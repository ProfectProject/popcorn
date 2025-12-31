package com.popcorn.demo.application.order.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.popcorn.demo.application.order.port.in.CreateOrderCommand;
import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
import com.popcorn.demo.application.order.port.out.FindOrderItemPricePort;
import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.ProcessOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderDomainService;

/**
	* CreateOrderUseCase 단위 테스트
	*
	* Clean Architecture의 Application Layer 테스트
	* - 비즈니스 흐름 조율 검증
	* - Port들의 상호작용 검증
	* - 외부 의존성을 Mock으로 대체
	*/
@ExtendWith(MockitoExtension.class)
@DisplayName("주문 생성 유스케이스 테스트")
class CreateOrderUseCaseTest {

	@Mock
	private OrderDomainService orderDomainService;
	@Mock
	private FindOrderPort findOrderPort;
	@Mock
	private SaveOrderPort saveOrderPort;
	@Mock
	private ProcessOrderPort processOrderPort;
	@Mock
	private FindOrderItemPricePort findOrderItemPricePort;
	@Mock
	private IdempotencyCache idempotencyCache;
	@Mock
	private ApplicationEventPublisher eventPublisher;

	private CreateOrderUseCase createOrderUseCase;

	@BeforeEach
	void setUp() {
		createOrderUseCase = new CreateOrderUseCase(
				orderDomainService,
				findOrderPort,
				saveOrderPort,
				processOrderPort,
				findOrderItemPricePort,
				idempotencyCache,
				eventPublisher
		);
	}

	@Test
	@DisplayName("주문 생성 성공 - 모든 단계가 순서대로 실행됨")
	void createOrder_ValidCommand_ReturnsSuccessResponse() {
		// given
		CreateOrderCommand command = createValidCommand();
		Order mockOrder = createMockOrder();
		Order savedOrder = createSavedOrder();

		when(idempotencyCache.isDuplicate(anyString())).thenReturn(false);
		when(findOrderPort.findByIdempotencyKey(anyString())).thenReturn(Mono.empty());
		when(processOrderPort.validateOrder(anyLong(), any(UUID.class), any())).thenReturn(Mono.just(true));
		when(findOrderItemPricePort.findSessionOptionPrice(any(UUID.class))).thenReturn(Mono.just(14500));
		when(orderDomainService.createOrder(any(), any(), any(), any(), any(), anyString())).thenReturn(mockOrder);
		when(saveOrderPort.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
		when(saveOrderPort.saveOrderItems(any())).thenReturn(Mono.empty());

		// when
		Mono<CreateOrderResponse> response = createOrderUseCase.createOrder(command);

		// then
		StepVerifier.create(response)
				.assertNext(result -> {
					assertThat(result.getOrderId()).isEqualTo(savedOrder.getId());
					assertThat(result.getOrderNo()).isEqualTo(savedOrder.getOrderNo());
					assertThat(result.getOrderType()).isEqualTo(savedOrder.getOrderType().name());
					assertThat(result.getStatus()).isEqualTo(savedOrder.getStatus().name());
				})
				.verifyComplete();

		verify(findOrderPort, times(1)).findByIdempotencyKey(command.getIdempotencyKey());
		verify(processOrderPort, times(1)).validateOrder(anyLong(), any(UUID.class), any());
		verify(orderDomainService, times(1)).createOrder(any(), any(), any(), any(), any(), any());
		verify(saveOrderPort, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("중복 주문 검증 실패 - 예외 발생")
	void createOrder_DuplicateOrder_ThrowsException() {
		// given
		CreateOrderCommand command = createValidCommand();
		Order existingOrder = createMockOrder();

		when(idempotencyCache.isDuplicate(anyString())).thenReturn(false);
		when(findOrderPort.findByIdempotencyKey(anyString())).thenReturn(Mono.just(existingOrder));
		when(orderDomainService.isDuplicateOrder(any(Optional.class), anyString())).thenReturn(true);

		// when
		Mono<CreateOrderResponse> response = createOrderUseCase.createOrder(command);

		// then
		StepVerifier.create(response)
				.expectError(OrderException.class)
				.verify();

		verify(findOrderPort, times(1)).findByIdempotencyKey(command.getIdempotencyKey());
		verify(orderDomainService, times(1)).isDuplicateOrder(any(Optional.class), any());
		verify(saveOrderPort, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("멱등성 키가 없는 경우 - 중복 검사를 건너뛰고 정상 처리")
	void createOrder_NoIdempotencyKey_SkipsDuplicateCheck() {
		// given
		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(1001L)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType("RESERVATION")
				.idempotencyKey(null)
				.items(createSampleItemCommands())
				.build();

		Order mockOrder = createMockOrder();
		Order savedOrder = createSavedOrder();

		when(processOrderPort.validateOrder(anyLong(), any(UUID.class), any())).thenReturn(Mono.just(true));
		when(findOrderItemPricePort.findSessionOptionPrice(any(UUID.class))).thenReturn(Mono.just(14500));
		when(orderDomainService.createOrder(any(), any(), any(), any(), any(), any())).thenReturn(mockOrder);
		when(saveOrderPort.save(any(Order.class))).thenReturn(Mono.just(savedOrder));
		when(saveOrderPort.saveOrderItems(any())).thenReturn(Mono.empty());

		// when
		Mono<CreateOrderResponse> response = createOrderUseCase.createOrder(command);

		// then
		StepVerifier.create(response)
				.assertNext(result -> assertThat(result.getOrderId()).isEqualTo(savedOrder.getId()))
				.verifyComplete();

		verify(findOrderPort, times(0)).findByIdempotencyKey(any());
		verify(orderDomainService, times(0)).isDuplicateOrder(any(Optional.class), any());
		verify(processOrderPort, times(1)).validateOrder(anyLong(), any(UUID.class), any());
		verify(orderDomainService, times(1)).createOrder(any(), any(), any(), any(), any(), any());
		verify(saveOrderPort, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("도메인 서비스에서 검증 실패 - 예외가 전파됨")
	void createOrder_DomainValidationFails_PropagatesException() {
		// given
		CreateOrderCommand command = createValidCommand();

		when(idempotencyCache.isDuplicate(anyString())).thenReturn(false);
		when(findOrderPort.findByIdempotencyKey(anyString())).thenReturn(Mono.empty());
		when(processOrderPort.validateOrder(anyLong(), any(UUID.class), any())).thenReturn(Mono.just(true));
		when(findOrderItemPricePort.findSessionOptionPrice(any(UUID.class))).thenReturn(Mono.just(14500));
		when(orderDomainService.createOrder(any(), any(), any(), any(), any(), any()))
				.thenThrow(OrderException.invalidRequest());

		// when
		Mono<CreateOrderResponse> response = createOrderUseCase.createOrder(command);

		// then
		StepVerifier.create(response)
				.expectError(OrderException.class)
				.verify();

		verify(saveOrderPort, never()).save(any(Order.class));
	}

	private CreateOrderCommand createValidCommand() {
		return CreateOrderCommand.builder()
				.userId(1001L)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType("RESERVATION")
				.idempotencyKey("test-key-001")
				.items(createSampleItemCommands())
				.build();
	}

	private List<CreateOrderCommand.OrderItemCommand> createSampleItemCommands() {
		return List.of(
				CreateOrderCommand.OrderItemCommand.builder()
						.orderItemType(OrderItemType.RESERVATION)
						.sessionId(UUID.randomUUID())
						.optionId(UUID.randomUUID())
						.qty(2)
						.unitPrice(14500)
						.build()
		);
	}

	private Order createMockOrder() {
		return Order.builder()
				.orderNo("O20231230-000001")
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(29000)
				.idempotencyKey("test-key-001")
				.cancelableUntil(LocalDateTime.now().plusDays(1))
				.orderItems(new ArrayList<>())
				.build();
	}

	private Order createSavedOrder() {
		Order order = createMockOrder();
		return Order.builder()
				.id(UUID.randomUUID())
				.orderNo(order.getOrderNo())
				.customerId(order.getCustomerId())
				.storeId(order.getStoreId())
				.productId(order.getProductId())
				.orderType(order.getOrderType())
				.status(order.getStatus())
				.totalAmount(order.getTotalAmount())
				.idempotencyKey(order.getIdempotencyKey())
				.cancelableUntil(order.getCancelableUntil())
				.orderItems(order.getOrderItems())
				.build();
	}
}
