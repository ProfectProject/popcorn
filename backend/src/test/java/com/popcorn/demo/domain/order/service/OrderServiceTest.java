package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.popcorn.demo.common.cache.IdempotencyCache;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderDomainService orderDomainService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderItemPriceService orderItemPriceService;

	@Mock
	private IdempotencyCache idempotencyCache;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private OrderService orderService;

	@BeforeEach
	void setUp() {
		orderService = new OrderService(
				orderDomainService,
				orderRepository,
				orderItemPriceService,
				idempotencyCache,
				eventPublisher
		);
	}

	@Nested
	@DisplayName("상태 검증")
	class StateVerification {

		@Test
		@DisplayName("주문 생성 - 응답 상태 확인")
		void createOrder_success_state() {
			CreateOrderCommand command = createReservationCommand();
			Order createdOrder = createOrderEntity(command);
			Order savedOrder = withId(createdOrder);

			when(idempotencyCache.isDuplicate(anyString())).thenReturn(false);
			when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
			when(orderItemPriceService.findSessionOptionPrice(any(UUID.class))).thenReturn(Optional.of(1000));
			when(orderDomainService.createOrder(any(), any(), any(), any(), any(), anyString()))
					.thenReturn(createdOrder);
			when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

			CreateOrderResponse response = orderService.createOrder(command);

			assertThat(response.getOrderId()).isEqualTo(savedOrder.getId());
			assertThat(response.getStatus()).isEqualTo(OrderStatus.REQUESTED.name());
		}

		@Test
		@DisplayName("주문 생성 - 옵션 가격 없음")
		void createOrder_optionPriceMissing() {
			CreateOrderCommand command = createReservationCommand();

			when(idempotencyCache.isDuplicate(anyString())).thenReturn(false);
			when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
			when(orderItemPriceService.findSessionOptionPrice(any(UUID.class))).thenReturn(Optional.empty());

			assertThatThrownBy(() -> orderService.createOrder(command))
					.isInstanceOf(OrderException.class)
					.satisfies(ex -> assertThat(((OrderException) ex).getResponseCode())
							.isEqualTo(OrderResponseCode.OPTION_NOT_FOUND));
		}

		@Test
		@DisplayName("주문 상태 변경 - 정상 전이")
		void updateStatus_success_state() {
			UUID orderId = UUID.randomUUID();
			Order order = Order.builder()
					.id(orderId)
					.status(OrderStatus.REQUESTED)
					.build();
			Order savedOrder = Order.builder()
					.id(orderId)
					.status(OrderStatus.OWNER_ACCEPTED)
					.build();

			when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
			when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
			when(orderDomainService.canChangeStatus(OrderStatus.REQUESTED, OrderStatus.OWNER_ACCEPTED)).thenReturn(true);

			Order result = orderService.updateStatus(orderId, "OWNER_ACCEPTED", "approved");

			assertThat(result.getStatus()).isEqualTo(OrderStatus.OWNER_ACCEPTED);
		}

		@Test
		@DisplayName("주문 상태 변경 - 허용되지 않은 전이")
		void updateStatus_invalidTransition() {
			UUID orderId = UUID.randomUUID();
			Order order = Order.builder()
					.id(orderId)
					.status(OrderStatus.READY)
					.build();

			when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
			when(orderDomainService.canChangeStatus(OrderStatus.READY, OrderStatus.OWNER_ACCEPTED)).thenReturn(false);

			assertThatThrownBy(() -> orderService.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
					.isInstanceOf(OrderException.class)
					.satisfies(ex -> assertThat(((OrderException) ex).getResponseCode())
							.isEqualTo(OrderResponseCode.INVALID_STATUS_TRANSITION));
		}
	}

	@Nested
	@DisplayName("상호작용 검증")
	class InteractionVerification {

		@Test
		@DisplayName("주문 생성 - 저장 및 이벤트 발행")
		void createOrder_success_interaction() {
			CreateOrderCommand command = createReservationCommand();
			Order createdOrder = createOrderEntity(command);
			Order savedOrder = withId(createdOrder);

			when(idempotencyCache.isDuplicate(anyString())).thenReturn(false);
			when(orderRepository.findByIdempotencyKey(anyString())).thenReturn(Optional.empty());
			when(orderItemPriceService.findSessionOptionPrice(any(UUID.class))).thenReturn(Optional.of(1000));
			when(orderDomainService.createOrder(any(), any(), any(), any(), any(), anyString()))
					.thenReturn(createdOrder);
			when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

			orderService.createOrder(command);

			verify(orderRepository).saveOrderItems(any());
			verify(eventPublisher).publishEvent(any(Object.class));
		}

		@Test
		@DisplayName("주문 생성 - 멱등성 키 중복 시 저장 안 함")
		void createOrder_duplicateIdempotency_interaction() {
			CreateOrderCommand command = createReservationCommand();

			when(idempotencyCache.isDuplicate(anyString())).thenReturn(true);

			assertThatThrownBy(() -> orderService.createOrder(command))
					.isInstanceOf(OrderException.class)
					.satisfies(ex -> assertThat(((OrderException) ex).getResponseCode())
							.isEqualTo(OrderResponseCode.DUPLICATE_IDEMPOTENCY_KEY));

			verify(orderRepository, never()).save(any());
		}

		@Test
		@DisplayName("주문 상태 변경 - 이력 저장")
		void updateStatus_success_interaction() {
			UUID orderId = UUID.randomUUID();
			Order order = Order.builder()
					.id(orderId)
					.status(OrderStatus.REQUESTED)
					.build();
			Order savedOrder = Order.builder()
					.id(orderId)
					.status(OrderStatus.OWNER_ACCEPTED)
					.build();

			when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
			when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
			when(orderDomainService.canChangeStatus(OrderStatus.REQUESTED, OrderStatus.OWNER_ACCEPTED)).thenReturn(true);

			orderService.updateStatus(orderId, "OWNER_ACCEPTED", "approved");

			verify(orderRepository).saveStatusHistory(any());
		}
	}

	private CreateOrderCommand createReservationCommand() {
		return CreateOrderCommand.builder()
				.userId(1001L)
				.storeId(UUID.randomUUID())
				.productId(UUID.randomUUID())
				.orderType("RESERVATION")
				.idempotencyKey("test-key-001")
				.items(List.of(
						CreateOrderCommand.OrderItemCommand.builder()
								.orderItemType(OrderItemType.RESERVATION)
								.sessionId(UUID.randomUUID())
								.optionId(UUID.randomUUID())
								.qty(2)
								.unitPrice(1000)
								.build()
				))
				.build();
	}

	private Order createOrderEntity(CreateOrderCommand command) {
		return Order.builder()
				.orderNo("O-1001")
				.customerId(command.getUserId())
				.storeId(command.getStoreId())
				.productId(command.getProductId())
				.orderType(OrderType.RESERVATION)
				.status(OrderStatus.REQUESTED)
				.totalAmount(2000)
				.idempotencyKey(command.getIdempotencyKey())
				.cancelableUntil(LocalDateTime.now().plusDays(1))
				.orderItems(List.of(
						OrderItem.builder()
								.id(UUID.randomUUID())
								.orderItemType(OrderItemType.RESERVATION)
								.qty(2)
								.unitPrice(1000)
								.lineAmount(2000)
								.build()
				))
				.build();
	}

	private Order withId(Order order) {
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
