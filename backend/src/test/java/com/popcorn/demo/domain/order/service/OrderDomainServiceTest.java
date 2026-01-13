package com.popcorn.demo.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.global.exception.BaseException;

/**
	* OrderDomainService 단위 테스트
	*
	* Clean Architecture의 Domain Layer 테스트
	* - 순수 비즈니스 로직 검증
	* - 외부 의존성 없이 테스트
	* - 도메인 규칙과 불변식 검증
	*/
@DisplayName("주문 도메인 서비스 테스트")
class OrderDomainServiceTest {

	private OrderDomainService orderDomainService;

	@BeforeEach
	void setUp() {
		orderDomainService = new OrderDomainService();
	}


	@Test
	@DisplayName("주문 생성 검증 - 정상적인 주문은 통과")
	void validateOrderCreation_ValidOrder_DoesNotThrow() {
		// given
		Long customerId = 1001L;
		UUID storeId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		List<OrderItem> orderItems = createSampleOrderItems();

		// when & then
		// when & then - 예외가 발생하지 않으면 성공
		try {
			orderDomainService.validateOrderCreation(customerId, storeId, popupId, orderItems);
		} catch (Exception e) {
			throw new AssertionError("예외가 발생하지 않아야 함", e);
		}
	}

	@Test
	@DisplayName("주문 생성 검증 - 고객 ID가 null이면 예외 발생")
	void validateOrderCreation_NullCustomerId_ThrowsException() {
		// given
		Long customerId = null;
		UUID storeId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		List<OrderItem> orderItems = createSampleOrderItems();

		// when & then
		assertThatThrownBy(() -> orderDomainService.validateOrderCreation(customerId, storeId, popupId, orderItems))
				.isInstanceOf(BaseException.class);
	}

	@Test
	@DisplayName("주문 생성 검증 - 스토어 ID가 null이면 예외 발생")
	void validateOrderCreation_NullStoreId_ThrowsException() {
		Long customerId = 1001L;
		UUID storeId = null;
		UUID popupId = UUID.randomUUID();
		List<OrderItem> orderItems = createSampleOrderItems();

		assertThatThrownBy(() -> orderDomainService.validateOrderCreation(customerId, storeId, popupId, orderItems))
				.isInstanceOf(BaseException.class);
	}

	@Test
	@DisplayName("주문 생성 검증 - 상품 ID가 null이면 예외 발생")
	void validateOrderCreation_NullProductId_ThrowsException() {
		Long customerId = 1001L;
		UUID storeId = UUID.randomUUID();
		UUID popupId = null;
		List<OrderItem> orderItems = createSampleOrderItems();

		assertThatThrownBy(() -> orderDomainService.validateOrderCreation(customerId, storeId, popupId, orderItems))
				.isInstanceOf(BaseException.class);
	}

	@Test
	@DisplayName("주문 생성 검증 - 주문 항목이 비어있으면 예외 발생")
	void validateOrderCreation_EmptyItems_ThrowsException() {
		// given
		Long customerId = 1001L;
		UUID storeId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		List<OrderItem> orderItems = new ArrayList<>();

		// when & then
		assertThatThrownBy(() -> orderDomainService.validateOrderCreation(customerId, storeId, popupId, orderItems))
				.isInstanceOf(BaseException.class);
	}

	@Test
	@DisplayName("취소 가능 시간 계산 - 예약형은 1일 후")
	void calculateCancelableUntil_Reservation_Returns1DayLater() {
		// given
		OrderType orderType = OrderType.RESERVATION;
		LocalDateTime before = LocalDateTime.now().plusDays(1).minusMinutes(1);
		LocalDateTime after = LocalDateTime.now().plusDays(1).plusMinutes(1);

		// when
		LocalDateTime cancelableUntil = orderDomainService.calculateCancelableUntil(orderType);

		// then
		assertThat(cancelableUntil).isBetween(before, after);
	}

	@Test
	@DisplayName("취소 가능 시간 계산 - 구매형은 1시간 후")
	void calculateCancelableUntil_Purchase_Returns1HourLater() {
		// given
		OrderType orderType = OrderType.PURCHASE;
		LocalDateTime before = LocalDateTime.now().plusHours(1).minusMinutes(1);
		LocalDateTime after = LocalDateTime.now().plusHours(1).plusMinutes(1);

		// when
		LocalDateTime cancelableUntil = orderDomainService.calculateCancelableUntil(orderType);

		// then
		assertThat(cancelableUntil).isBetween(before, after);
	}

	@Test
	@DisplayName("총액 계산 - 수량과 단가의 곱셈 합계")
	void calculateTotalAmount_MultipleItems_ReturnsCorrectSum() {
		// given
		List<OrderItem> orderItems = List.of(
				createOrderItem(10000, 2), // 20,000
				createOrderItem(15000, 1)  // 15,000
		);

		// when
		int totalAmount = orderDomainService.calculateTotalAmount(orderItems);

		// then
		assertThat(totalAmount).isEqualTo(35000);
	}

	@Test
	@DisplayName("주문 엔티티 생성 - 모든 필드가 올바르게 설정됨")
	void createOrder_ValidInput_CreatesOrderWithCorrectFields() {
		// given
		Long customerId = 1001L;
		UUID storeId = UUID.randomUUID();
		UUID popupId = UUID.randomUUID();
		OrderType orderType = OrderType.RESERVATION;
		List<OrderItem> orderItems = createSampleOrderItems();

		// when
		Order order = orderDomainService.createOrder(
				customerId,
				storeId,
				popupId,
				orderType,
				orderItems
		);

		// then
		assertThat(order).isNotNull();
		assertThat(order.getCustomerId()).isEqualTo(customerId);
		assertThat(order.getStoreId()).isEqualTo(storeId);
		assertThat(order.getPopupId()).isEqualTo(popupId);
		assertThat(order.getOrderType()).isEqualTo(orderType);
		assertThat(order.getStatus()).isEqualTo(OrderStatus.REQUESTED);
		assertThat(order.getOrderNo()).isNotEmpty();
		assertThat(order.getTotalAmount()).isPositive();
		assertThat(order.getCancelableUntil()).isAfter(LocalDateTime.now());
		assertThat(order.getOrderItems()).hasSize(orderItems.size());
	}

	@Test
	@DisplayName("주문 상태 변경 가능 검증 - REQUESTED에서 RESERVED로 변경 가능")
	void canChangeStatus_RequestedToConfirmed_ReturnsTrue() {
		// given
		OrderStatus currentStatus = OrderStatus.REQUESTED;
		OrderStatus newStatus = OrderStatus.RESERVED;

		// when
		boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

		// then
		assertThat(canChange).isTrue();
	}

	@Test
	@DisplayName("주문 상태 변경 가능 검증 - ACCEPTED에서 RESERVED로 변경 가능")
	void canChangeStatus_OwnerAcceptedToConfirmed_ReturnsTrue() {
		// given
		OrderStatus currentStatus = OrderStatus.ACCEPTED;
		OrderStatus newStatus = OrderStatus.RESERVED;

		// when
		boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

		// then
		assertThat(canChange).isTrue();
	}

	@Test
	@DisplayName("주문 상태 변경 가능 검증 - RESERVED에서 PAYMENT_PENDING으로 변경 가능")
	void canChangeStatus_ConfirmedToPreparing_ReturnsTrue() {
		// given
		OrderStatus currentStatus = OrderStatus.RESERVED;
		OrderStatus newStatus = OrderStatus.PAYMENT_PENDING;

		// when
		boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

		// then
		assertThat(canChange).isTrue();
	}

	@Test
	@DisplayName("주문 상태 변경 가능 검증 - REQUESTED에서 PAYMENT_PENDING로 변경 가능")
	void canChangeStatus_RequestedToPending_ReturnsTrue() {
		// given
		OrderStatus currentStatus = OrderStatus.REQUESTED;
		OrderStatus newStatus = OrderStatus.PAYMENT_PENDING;

		// when
		boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

		// then
		assertThat(canChange).isTrue();
	}

	@Test
	@DisplayName("주문 상태 변경 가능 검증 - COMPLETED에서 변경 불가")
	void canChangeStatus_CompletedToAny_ReturnsFalse() {
		// given
		OrderStatus currentStatus = OrderStatus.COMPLETED;
		OrderStatus newStatus = OrderStatus.CANCELLED;

		// when
		boolean canChange = orderDomainService.canChangeStatus(currentStatus, newStatus);

		// then
		assertThat(canChange).isFalse();
	}

	// Helper methods
	private List<OrderItem> createSampleOrderItems() {
		return List.of(
				createOrderItem(14500, 2)
		);
	}

	private OrderItem createOrderItem(int unitPrice, int qty) {
		return OrderItem.builder()
				.orderItemType(OrderItemType.RESERVATION)
				.qty(qty)
				.unitPrice(unitPrice)
				.lineAmount(unitPrice * qty)
				.sessionOptionId(UUID.randomUUID())
				.build();
	}
}
