package com.popcorn.demo.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.common.dto.CommonResponseCode;
import com.popcorn.demo.domain.order.dto.OrderResponseCode;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.global.exception.BaseException;

@ExtendWith(MockitoExtension.class)
@DisplayName("결제 기록 생성 서비스 테스트")
class PaymentCommandServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private OrderCommandService orderCommandService;

	@Mock
	private JpaPaymentRepository paymentRepository;

	@Mock
	private JpaOrderItemRepository orderItemRepository;

	private PaymentCommandService paymentCommandService;

	@BeforeEach
	void setUp() {
		paymentCommandService = new PaymentCommandService(
				orderRepository, orderCommandService, paymentRepository, orderItemRepository);
	}

	@Test
	@DisplayName("성공: 예약 결제 기록 생성")
	void createReservationPayment_success() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004003");

		Order order = createOrder(orderId, OrderType.RESERVATION, OrderStatus.REQUESTED);
		Payment savedPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(4000)
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
		when(orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId)).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createReservationPayment(orderId, "CARD", 4000, null);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
		assertThat(result.getApprovedAt()).isNull();
	}

	@Test
	@DisplayName("성공: 구매 결제 기록 생성")
	void createOrderPayment_success() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001004");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004004");

		Order order = createOrder(orderId, OrderType.PURCHASE, OrderStatus.REQUESTED);
		Payment savedPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(3000)
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId)).thenReturn(true);
		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createOrderPayment(orderId, "CARD", 3000, null);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
		assertThat(result.getApprovedAt()).isNull();
	}

	@Test
	@DisplayName("성공: READY 결제 기록 생성")
	void createReadyPayment_success() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001005");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004005");

		Order order = createOrder(orderId, OrderType.PURCHASE, OrderStatus.REQUESTED);
		Payment savedPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(3000)
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
		when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

		PaymentCommandService.PaymentCreationResult result =
				paymentCommandService.createReadyPayment(orderId, "TRANSFER", 3000, null);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.READY);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.REQUESTED);
		assertThat(result.getApprovedAt()).isNull();
	}

	@Test
	@DisplayName("실패: 예약 결제 수단이 허용되지 않음")
	void createReservationPayment_invalidMethod() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		Order order = createOrder(orderId, OrderType.RESERVATION, OrderStatus.REQUESTED);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
		when(orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId)).thenReturn(false);

		BaseException exception = assertThrows(BaseException.class,
				() -> paymentCommandService.createReservationPayment(orderId, "VIRTUAL", 4000, null));

		assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("실패: 구매 결제는 CARD만 허용")
	void createOrderPayment_invalidMethod() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001004");
		Order order = createOrder(orderId, OrderType.PURCHASE, OrderStatus.REQUESTED);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId)).thenReturn(true);

		BaseException exception = assertThrows(BaseException.class,
				() -> paymentCommandService.createOrderPayment(orderId, "CASH", 3000, null));

		assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("실패: 결제 기록 중복")
	void createPayment_alreadyExists() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		Order order = createOrder(orderId, OrderType.RESERVATION, OrderStatus.REQUESTED);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.existsByOrderIdAndDeletedAtIsNull(orderId)).thenReturn(true);

		PaymentException exception = assertThrows(PaymentException.class,
				() -> paymentCommandService.createReservationPayment(orderId, "CARD", 4000, null));

		assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.PAYMENT_ALREADY_EXISTS);
	}

	@Test
	@DisplayName("실패: 주문 타입 불일치")
	void createReservationPayment_orderTypeMismatch() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001004");
		Order order = createOrder(orderId, OrderType.PURCHASE, OrderStatus.REQUESTED);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(false);
		when(orderItemRepository.existsByOrderIdAndGoodsVariantIdIsNotNull(orderId)).thenReturn(true);

		BaseException exception = assertThrows(BaseException.class,
				() -> paymentCommandService.createReservationPayment(orderId, "CARD", 4000, null));

		assertThat(exception.getResponseCode()).isEqualTo(CommonResponseCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("실패: 취소된 주문 결제 시도")
	void createPayment_cancelledOrder() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		Order order = createOrder(orderId, OrderType.RESERVATION, OrderStatus.CANCELLED);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

		OrderValidationException exception = assertThrows(OrderValidationException.class,
				() -> paymentCommandService.createReservationPayment(orderId, "CARD", 4000, null));

		assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.INVALID_STATUS_TRANSITION);
	}

	@Test
	@DisplayName("실패: 주문을 찾을 수 없음")
	void createPayment_orderNotFound() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000009999");

		when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

		OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
				() -> paymentCommandService.createReservationPayment(orderId, "CARD", 4000, null));

		assertThat(exception.getResponseCode()).isEqualTo(OrderResponseCode.ORDER_NOT_FOUND);
	}

	private Order createOrder(UUID orderId, OrderType orderType, OrderStatus status) {
		return Order.builder()
				.id(orderId)
				.orderType(orderType)
				.status(status)
				.build();
	}
}
