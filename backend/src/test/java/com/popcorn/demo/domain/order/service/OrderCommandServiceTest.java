package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderConflictException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.repository.OrderRepository;

class OrderCommandServiceTest {

	@Test
	@DisplayName("Creates order with validation and event publishing")
	void createOrderSuccess() {
		OrderDomainService domainService = Mockito.mock(OrderDomainService.class);
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		OrderItemPriceService itemPriceService = Mockito.mock(OrderItemPriceService.class);
		AsyncEventPublisher eventPublisher = Mockito.mock(AsyncEventPublisher.class);
		OrderValidationService validationService = Mockito.mock(OrderValidationService.class);

		OrderCommandService service = new OrderCommandService(
				domainService, orderRepository, itemPriceService, eventPublisher, validationService);

		UUID popupId = UUID.randomUUID();
		UUID storeId = UUID.randomUUID();
		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(1001L)
				.popupId(popupId)
				.orderType("RESERVATION")
				.items(List.of(
						CreateOrderCommand.OrderItemCommand.builder()
								.orderItemType(OrderItemType.RESERVATION)
								.sessionId(UUID.randomUUID())
								.qty(2)
								.build(),
						CreateOrderCommand.OrderItemCommand.builder()
								.orderItemType(OrderItemType.GOODS)
								.goodsVariantId(UUID.randomUUID())
								.qty(1)
								.build()
				))
				.build();

		when(validationService.validateOrderAsync(Mockito.eq(1001L), Mockito.eq(popupId), Mockito.eq(3)))
				.thenReturn(true);
		when(validationService.resolveStoreId(popupId)).thenReturn(storeId);
		when(itemPriceService.findSessionOptionPrice(Mockito.any())).thenReturn(Optional.of(10000));
		when(itemPriceService.findMerchVariantPrice(Mockito.any())).thenReturn(Optional.of(5000));
		when(eventPublisher.publishEventAsync(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));

		when(domainService.createOrder(Mockito.eq(1001L), Mockito.eq(storeId), Mockito.eq(popupId),
				Mockito.eq(OrderType.RESERVATION), Mockito.anyList()))
				.thenAnswer(invocation -> {
					@SuppressWarnings("unchecked")
					List<OrderItem> items = (List<OrderItem>) invocation.getArgument(4);
					return Order.builder()
							.id(UUID.randomUUID())
							.orderNo("O-1001")
							.customerId(1001L)
							.storeId(storeId)
							.popupId(popupId)
							.orderType(OrderType.RESERVATION)
							.status(OrderStatus.REQUESTED)
							.totalAmount(25000)
							.orderItems(items)
							.build();
				});

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<OrderItem>> orderItemsCaptor = ArgumentCaptor.forClass(List.class);
		when(orderRepository.save(Mockito.any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CreateOrderResponse response = service.createOrder(command);

		assertThat(response.getOrderNo()).isEqualTo("O-1001");
		verify(domainService).validateOrderCreation(Mockito.eq(1001L), Mockito.eq(storeId),
				Mockito.eq(popupId), orderItemsCaptor.capture());
		verify(orderRepository).saveOrderItems(Mockito.anyList());
		verify(orderRepository).saveStatusHistory(Mockito.any());
		assertThat(orderItemsCaptor.getValue()).hasSize(2);
	}

	@Test
	@DisplayName("Rejects order when async validation fails")
	void createOrderRejectsInvalidRequest() {
		OrderDomainService domainService = Mockito.mock(OrderDomainService.class);
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		OrderItemPriceService itemPriceService = Mockito.mock(OrderItemPriceService.class);
		AsyncEventPublisher eventPublisher = Mockito.mock(AsyncEventPublisher.class);
		OrderValidationService validationService = Mockito.mock(OrderValidationService.class);

		OrderCommandService service = new OrderCommandService(
				domainService, orderRepository, itemPriceService, eventPublisher, validationService);

		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(1001L)
				.popupId(UUID.randomUUID())
				.orderType("RESERVATION")
				.items(List.of(CreateOrderCommand.OrderItemCommand.builder()
						.orderItemType(OrderItemType.RESERVATION)
						.sessionId(UUID.randomUUID())
						.qty(1)
						.build()))
				.build();

		when(validationService.validateOrderAsync(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(false);
		when(itemPriceService.findSessionOptionPrice(Mockito.any())).thenReturn(Optional.of(1000));

		assertThatThrownBy(() -> service.createOrder(command))
				.isInstanceOf(OrderValidationException.class);
		verify(orderRepository, Mockito.never()).save(Mockito.any(Order.class));
	}

	@Test
	@DisplayName("Throws conflict on duplicate order creation")
	void createOrderHandlesDuplicate() {
		OrderDomainService domainService = Mockito.mock(OrderDomainService.class);
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		OrderItemPriceService itemPriceService = Mockito.mock(OrderItemPriceService.class);
		AsyncEventPublisher eventPublisher = Mockito.mock(AsyncEventPublisher.class);
		OrderValidationService validationService = Mockito.mock(OrderValidationService.class);

		OrderCommandService service = new OrderCommandService(
				domainService, orderRepository, itemPriceService, eventPublisher, validationService);

		CreateOrderCommand command = CreateOrderCommand.builder()
				.userId(1001L)
				.popupId(UUID.randomUUID())
				.orderType("RESERVATION")
				.items(List.of(CreateOrderCommand.OrderItemCommand.builder()
						.orderItemType(OrderItemType.RESERVATION)
						.sessionId(UUID.randomUUID())
						.qty(1)
						.build()))
				.build();

		when(validationService.validateOrderAsync(Mockito.any(), Mockito.any(), Mockito.any()))
				.thenReturn(true);
		when(validationService.resolveStoreId(Mockito.any())).thenReturn(UUID.randomUUID());
		when(itemPriceService.findSessionOptionPrice(Mockito.any())).thenReturn(Optional.of(1000));
		when(domainService.createOrder(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyList()))
				.thenReturn(Order.builder()
						.id(UUID.randomUUID())
						.orderNo("O-2001")
						.customerId(1001L)
						.storeId(UUID.randomUUID())
						.popupId(UUID.randomUUID())
						.orderType(OrderType.RESERVATION)
						.status(OrderStatus.REQUESTED)
						.totalAmount(1000)
						.orderItems(List.of())
						.build());
		when(orderRepository.save(Mockito.any(Order.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate"));

		assertThatThrownBy(() -> service.createOrder(command))
				.isInstanceOf(OrderConflictException.class);
	}

	@Test
	@DisplayName("Updates order status and publishes cancel event")
	void updateStatusPublishesCancelEvent() {
		OrderDomainService domainService = Mockito.mock(OrderDomainService.class);
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		AsyncEventPublisher eventPublisher = Mockito.mock(AsyncEventPublisher.class);

		OrderCommandService service = new OrderCommandService(
				domainService,
				orderRepository,
				Mockito.mock(OrderItemPriceService.class),
				eventPublisher,
				Mockito.mock(OrderValidationService.class));

		Order order = Order.builder()
				.id(UUID.randomUUID())
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.status(OrderStatus.REQUESTED)
				.orderItems(List.of())
				.totalAmount(1000)
				.build();

		when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
		when(domainService.canChangeStatus(OrderStatus.REQUESTED, OrderStatus.CANCELLED)).thenReturn(true);
		when(orderRepository.save(Mockito.any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventPublisher.publishEventAsync(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));

		Order updated = service.updateStatus(order.getId(), "CANCELLED", "reason");

		assertThat(updated.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		verify(orderRepository).saveStatusHistory(Mockito.any());
	}

	@Test
	@DisplayName("Updates order status and publishes completed event")
	void updateStatusPublishesCompletedEvent() {
		OrderDomainService domainService = Mockito.mock(OrderDomainService.class);
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		AsyncEventPublisher eventPublisher = Mockito.mock(AsyncEventPublisher.class);

		OrderCommandService service = new OrderCommandService(
				domainService,
				orderRepository,
				Mockito.mock(OrderItemPriceService.class),
				eventPublisher,
				Mockito.mock(OrderValidationService.class));

		Order order = Order.builder()
				.id(UUID.randomUUID())
				.customerId(1001L)
				.storeId(UUID.randomUUID())
				.status(OrderStatus.RESERVED)
				.orderItems(List.of(OrderItem.builder().orderItemType(OrderItemType.GOODS).qty(1).build()))
				.totalAmount(1000)
				.build();
		order.setCreatedAt(LocalDateTime.now().minusMinutes(10));

		when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
		when(domainService.canChangeStatus(OrderStatus.RESERVED, OrderStatus.COMPLETED)).thenReturn(true);
		when(orderRepository.save(Mockito.any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(eventPublisher.publishEventAsync(Mockito.any())).thenReturn(CompletableFuture.completedFuture(null));

		Order updated = service.updateStatus(order.getId(), "COMPLETED", "done");

		assertThat(updated.getStatus()).isEqualTo(OrderStatus.COMPLETED);
		verify(orderRepository).saveStatusHistory(Mockito.any());
	}

	@Test
	@DisplayName("Rejects updates for cancelled or invalid status")
	void updateStatusRejectsInvalid() {
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		OrderCommandService service = new OrderCommandService(
				Mockito.mock(OrderDomainService.class),
				orderRepository,
				Mockito.mock(OrderItemPriceService.class),
				Mockito.mock(AsyncEventPublisher.class),
				Mockito.mock(OrderValidationService.class));

		Order cancelled = Order.builder()
				.id(UUID.randomUUID())
				.status(OrderStatus.CANCELLED)
				.build();
		when(orderRepository.findById(cancelled.getId())).thenReturn(Optional.of(cancelled));

		assertThatThrownBy(() -> service.updateStatus(cancelled.getId(), "REQUESTED", "no"))
				.isInstanceOf(OrderConflictException.class);
	}

	@Test
	@DisplayName("Rejects updates for invalid status name")
	void updateStatusRejectsInvalidStatusName() {
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		OrderDomainService domainService = Mockito.mock(OrderDomainService.class);
		OrderCommandService service = new OrderCommandService(
				domainService,
				orderRepository,
				Mockito.mock(OrderItemPriceService.class),
				Mockito.mock(AsyncEventPublisher.class),
				Mockito.mock(OrderValidationService.class));

		Order order = Order.builder()
				.id(UUID.randomUUID())
				.status(OrderStatus.REQUESTED)
				.build();
		when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> service.updateStatus(order.getId(), "INVALID", "reason"))
				.isInstanceOf(OrderValidationException.class);
		verify(domainService, Mockito.never()).canChangeStatus(Mockito.any(), Mockito.any());
	}

	@Test
	@DisplayName("Deletes all orders via repository")
	void deleteAllOrdersDelegates() {
		OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
		OrderCommandService service = new OrderCommandService(
				Mockito.mock(OrderDomainService.class),
				orderRepository,
				Mockito.mock(OrderItemPriceService.class),
				Mockito.mock(AsyncEventPublisher.class),
				Mockito.mock(OrderValidationService.class));

		service.deleteAllOrders();

		verify(orderRepository).deleteAllOrders();
	}
}
