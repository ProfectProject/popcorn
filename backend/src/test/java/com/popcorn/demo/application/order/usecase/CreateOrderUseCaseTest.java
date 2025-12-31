package com.popcorn.demo.application.order.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.popcorn.demo.application.order.port.in.CreateOrderCommand;
import com.popcorn.demo.application.order.port.in.CreateOrderResponse;
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

		when(findOrderPort.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
		when(orderDomainService.isDuplicateOrder(any(), anyString())).thenReturn(false);
		when(processOrderPort.validateOrder(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
		when(orderDomainService.createOrder(any(), any(), any(), any(), any(), anyString())).thenReturn(mockOrder);
		when(saveOrderPort.save(any(Order.class))).thenReturn(savedOrder);

		// when
		CreateOrderResponse response = createOrderUseCase.createOrder(command);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getOrderId()).isEqualTo(savedOrder.getId());
		assertThat(response.getOrderNo()).isEqualTo(savedOrder.getOrderNo());
		assertThat(response.getOrderType()).isEqualTo(savedOrder.getOrderType().name());
		assertThat(response.getStatus()).isEqualTo(savedOrder.getStatus().name());

		// 모든 필수 메서드들이 호출되었는지 검증
		verify(findOrderPort, times(1)).findByIdempotencyKey(command.getIdempotencyKey());
		verify(orderDomainService, times(1)).isDuplicateOrder(any(), any());
		verify(processOrderPort, times(1)).validateOrder(any(), any(), any());
		verify(orderDomainService, times(1)).createOrder(any(), any(), any(), any(), any(), any());
		verify(saveOrderPort, times(1)).save(any(Order.class));
		// processOrderPostActions는 이벤트를 통해 비동기로 호출되므로 직접 검증하지 않음
	}

	@Test
	@DisplayName("중복 주문 검증 실패 - 예외 발생")
	void createOrder_DuplicateOrder_ThrowsException() {
		// given
		CreateOrderCommand command = createValidCommand();
		Order existingOrder = createMockOrder();

		when(findOrderPort.findByIdempotencyKey(anyString())).thenReturn(Optional.of(existingOrder));
		when(orderDomainService.isDuplicateOrder(any(), anyString())).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> createOrderUseCase.createOrder(command))
				.isInstanceOf(OrderException.class);

		// 중복 검증 후 더 이상 진행되지 않았는지 확인
		verify(findOrderPort, times(1)).findByIdempotencyKey(command.getIdempotencyKey());
		verify(orderDomainService, times(1)).isDuplicateOrder(any(), any());
		verify(orderDomainService, times(0)).createOrder(any(), any(), any(), any(), any(), any());
		verify(saveOrderPort, times(0)).save(any(Order.class));
	}

	@Test
	@DisplayName("멱등성 키가 없는 경우 - 중복 검사를 건너뛰고 정상 처리")
	void createOrder_NoIdempotencyKey_SkipsDuplicateCheck() {
		// given
		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(1001L)
				.storeId(1L)
				.productId(1L)
				.orderType("RESERVATION")
				.idempotencyKey(null) // 멱등성 키 없음
				.items(createSampleItemCommands())
				.build();

		Order mockOrder = createMockOrder();
		Order savedOrder = createSavedOrder();

		when(processOrderPort.validateOrder(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
		when(orderDomainService.createOrder(any(), any(), any(), any(), any(), any())).thenReturn(mockOrder);
		when(saveOrderPort.save(any(Order.class))).thenReturn(savedOrder);

		// when
		CreateOrderResponse response = createOrderUseCase.createOrder(command);

		// then
		assertThat(response).isNotNull();

		// 멱등성 검사를 하지 않았는지 확인
		verify(findOrderPort, times(0)).findByIdempotencyKey(any());
		verify(orderDomainService, times(0)).isDuplicateOrder(any(), any());

		// 나머지 프로세스는 정상 진행되었는지 확인
		verify(processOrderPort, times(1)).validateOrder(any(), any(), any());
		verify(orderDomainService, times(1)).createOrder(any(), any(), any(), any(), any(), any());
		verify(saveOrderPort, times(1)).save(any(Order.class));
		// processOrderPostActions는 이벤트를 통해 비동기로 호출되므로 직접 검증하지 않음
	}

	@Test
	@DisplayName("도메인 서비스에서 검증 실패 - 예외가 전파됨")
	void createOrder_DomainValidationFails_PropagatesException() {
		// given
		CreateOrderCommand command = createValidCommand();

		when(findOrderPort.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
		when(orderDomainService.isDuplicateOrder(any(), anyString())).thenReturn(false);
		when(processOrderPort.validateOrder(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(true));
		when(orderDomainService.createOrder(any(), any(), any(), any(), any(), any()))
				.thenThrow(OrderException.invalidRequest());

		// when & then
		assertThatThrownBy(() -> createOrderUseCase.createOrder(command))
				.isInstanceOf(OrderException.class);

		// 도메인 검증 실패 후 저장이 시도되지 않았는지 확인
		verify(saveOrderPort, times(0)).save(any(Order.class));
		verify(processOrderPort, times(0)).processOrderPostActions(any());
	}

	// Helper methods
	private CreateOrderCommand createValidCommand() {
		return CreateOrderCommand.builder()
				.userId(1001L)
				.storeId(1L)
				.productId(1L)
				.orderType("RESERVATION")
				.idempotencyKey("test-key-001")
				.items(createSampleItemCommands())
				.build();
	}

	private List<CreateOrderCommand.OrderItemCommand> createSampleItemCommands() {
		return List.of(
				CreateOrderCommand.OrderItemCommand.builder()
						.orderItemType(OrderItemType.RESERVATION)
						.sessionId(1L)
						.qty(2)
						.unitPrice(14500)
						.build()
		);
	}

	private Order createMockOrder() {
		return Order.builder()
				.orderNo("O20231230-000001")
				.customerId(1001L)
				.storeId(1L)
				.productId(1L)
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
		order = Order.builder()
				.id(101L) // DB에 저장 후 ID 부여
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
		return order;
	}
}
