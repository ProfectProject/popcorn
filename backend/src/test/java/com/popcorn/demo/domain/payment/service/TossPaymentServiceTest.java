package com.popcorn.demo.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.exception.OrderValidationException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.payment.toss.TossPaymentsClient;
import com.popcorn.demo.domain.payment.toss.TossPaymentsConfirmResponse;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelRequest;
import com.popcorn.demo.domain.payment.toss.TossPaymentsCancelResponse;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
import com.popcorn.demo.domain.payment.event.PaymentSuccessEvent;
import com.popcorn.demo.common.cache.IdempotencyService;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("토스 결제 승인 서비스 테스트")
class TossPaymentServiceTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private JpaPaymentRepository paymentRepository;

	@Mock
	private OrderCommandService orderCommandService;

	@Mock
	private JpaOrderItemRepository orderItemRepository;

	@Mock
	private PaymentCommandService paymentCommandService;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	@Mock
	private TossPaymentsClient tossPaymentsClient;

	@Mock
	private IdempotencyService idempotencyService;

	private TossPaymentService tossPaymentService;

	@BeforeEach
	void setUp() {
		tossPaymentService = new TossPaymentService(
				orderRepository,
				paymentRepository,
				orderCommandService,
				paymentCommandService,
				orderItemRepository,
				tossPaymentsClient,
				new ObjectMapper(),
				eventPublisher,
				idempotencyService);
	}

	@Test
	@DisplayName("성공: 토스 결제 승인 처리")
	void confirmPayment_success() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004003");
		String orderNo = "O20251231-001003";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(4000)
				.build();
		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(4000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		// 새로운 결제 확인 시나리오: 기존 결제가 없는 상황
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(java.util.List.of());

		// PaymentCommandService.createPayment 호출 시 새 결제 생성
		PaymentCommandService.PaymentCreationResult paymentCreationResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(4000)
						.build();
		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(4000), any(String.class)))
				.thenReturn(paymentCreationResult);
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(confirmResponse(orderNo));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_123", orderNo, 4000);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(result.getApprovedAt()).isNotNull();
	}

	private TossPaymentsConfirmResponse confirmResponse(String orderNo) {
		TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
		response.setPaymentKey("pay_123");
		response.setOrderId(orderNo);
		response.setTotalAmount(4000);
		response.setStatus("DONE");
		response.setApprovedAt(LocalDateTime.now().toString());
		return response;
	}

	@Test
	@DisplayName("성공: UUID orderId로 주문 찾기")
	void confirmPayment_withUuidOrderId() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-001")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(5000)
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_uuid")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(5000)
						.build();

		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(5000), any()))
				.thenReturn(paymentResult);

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(5000)
				.build();

		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(confirmResponseWithAmount(5000));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_uuid", orderId.toString(), 5000);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	@DisplayName("실패: 주문을 찾을 수 없음")
	void confirmPayment_orderNotFound() {
		UUID orderId = UUID.randomUUID();
		when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
		lenient().when(orderRepository.findByOrderNo(orderId.toString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_key", orderId.toString(), 1000))
				.isInstanceOf(OrderNotFoundException.class);
	}

	@Test
	@DisplayName("성공: 동일한 paymentKey로 이미 처리된 결제가 있는 경우 (중복 차단)")
	void confirmPayment_duplicatePaymentKey() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Payment existingPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(3000)
				.approvedAt(LocalDateTime.now().minusMinutes(10))
				.build();

		Order existingOrder = Order.builder()
				.id(orderId)
				.orderNo("O20251231-002")
				.status(OrderStatus.PAID)
				.totalAmount(3000)
				.build();

		when(paymentRepository.findByPaymentKeyInRawPayload("pay_duplicate")).thenReturn(List.of(existingPayment));
		when(orderRepository.findById(orderId)).thenReturn(Optional.of(existingOrder));
		when(orderRepository.findByOrderNo("O20251231-002")).thenReturn(Optional.of(existingOrder));

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_duplicate", "O20251231-002", 3000);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);
		assertThat(result.getAmount()).isEqualTo(3000);

		// 새로운 결제 생성이나 토스 API 호출이 없어야 함
		verify(paymentCommandService, never()).createPayment(any(), any(), any(), any());
		verify(tossPaymentsClient, never()).confirm(any());
	}

	@Test
	@DisplayName("성공: 이미 결제 완료된 주문 (멱등성)")
	void confirmPayment_alreadyPaid() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-003")
				.status(OrderStatus.PAID)
				.totalAmount(2000)
				.build();

		Payment existingPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(2000)
				.approvedAt(LocalDateTime.now().minusMinutes(5))
				.build();

		when(orderRepository.findByOrderNo("O20251231-003")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_idempotent")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(existingPayment));

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_idempotent", "O20251231-003", 2000);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
		assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PAID);

		// 중복 처리 방지 확인
		verify(tossPaymentsClient, never()).confirm(any());
		verify(paymentCommandService, never()).createPayment(any(), any(), any(), any());
	}

	@Test
	@DisplayName("실패: 진행 중인 결제가 있는 경우 (중복 결제 차단)")
	void confirmPayment_duplicatePaymentAttempt() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-004")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(1500)
				.build();

		Payment existingPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(1500)
				.build();

		when(orderRepository.findByOrderNo("O20251231-004")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_in_progress")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(existingPayment));

		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_in_progress", "O20251231-004", 1500))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("실패: 토스 API 응답이 null인 경우")
	void confirmPayment_tossResponseNull() {
		UUID orderId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-005")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(1000)
				.build();

		when(orderRepository.findByOrderNo("O20251231-005")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_null_response")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());
		when(tossPaymentsClient.confirm(any())).thenReturn(null);

		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_null_response", "O20251231-005", 1000))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("실패: 결제 금액 불일치")
	void confirmPayment_amountMismatch() {
		UUID orderId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-006")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(1000)
				.build();

		TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
		response.setPaymentKey("pay_mismatch");
		response.setOrderId("O20251231-006");
		response.setTotalAmount(1500); // 다른 금액
		response.setStatus("DONE");

		when(orderRepository.findByOrderNo("O20251231-006")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_mismatch")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());
		when(tossPaymentsClient.confirm(any())).thenReturn(response);

		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_mismatch", "O20251231-006", 1000))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("성공: parseApprovedAt - null 또는 빈 문자열인 경우 현재 시간 반환")
	void parseApprovedAt_nullOrEmpty() {
		// parseApprovedAt은 private 메서드이므로 간접적으로 테스트
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-007")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(1000)
				.build();

		TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
		response.setPaymentKey("pay_null_approved");
		response.setOrderId("O20251231-007");
		response.setTotalAmount(1000);
		response.setStatus("DONE");
		response.setApprovedAt(null); // null로 설정

		when(orderRepository.findByOrderNo("O20251231-007")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_null_approved")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(1000)
						.build();

		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(1000), any()))
				.thenReturn(paymentResult);

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(1000)
				.build();

		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(response);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_null_approved", "O20251231-007", 1000);

		assertThat(result.getApprovedAt()).isNotNull();
		assertThat(result.getApprovedAt()).isBeforeOrEqualTo(LocalDateTime.now());
	}

	@Test
	@DisplayName("성공: parseApprovedAt - 잘못된 날짜 형식인 경우 현재 시간 반환")
	void parseApprovedAt_invalidFormat() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-008")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(1000)
				.build();

		TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
		response.setPaymentKey("pay_invalid_date");
		response.setOrderId("O20251231-008");
		response.setTotalAmount(1000);
		response.setStatus("DONE");
		response.setApprovedAt("invalid-date-format");

		when(orderRepository.findByOrderNo("O20251231-008")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_invalid_date")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(1000)
						.build();

		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(1000), any()))
				.thenReturn(paymentResult);

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(1000)
				.build();

		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(response);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_invalid_date", "O20251231-008", 1000);

		assertThat(result.getApprovedAt()).isNotNull();
		assertThat(result.getApprovedAt()).isBeforeOrEqualTo(LocalDateTime.now());
	}

	@Test
	@DisplayName("성공: publishPaymentSuccessEvent - 이벤트 발행 성공")
	void publishPaymentSuccessEvent_success() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-009")
				.status(OrderStatus.PAYMENT_PENDING)
				.customerId(1L)
				.orderType(OrderType.RESERVATION)
				.totalAmount(2000)
				.build();

		List<OrderItem> orderItems = List.of(
				OrderItem.builder()
						.id(UUID.randomUUID())
						.orderId(orderId)
						.qty(1)
						.unitPrice(2000)
						.lineAmount(2000)
						.build()
		);

		when(orderRepository.findByOrderNo("O20251231-009")).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_event")).thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());
		when(orderItemRepository.findByOrderId(orderId)).thenReturn(orderItems);

		PaymentCommandService.PaymentCreationResult paymentResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(2000)
						.build();

		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(2000), any()))
				.thenReturn(paymentResult);

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(2000)
				.build();

		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(confirmResponseWithAmount(2000));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		doNothing().when(eventPublisher).publishEvent(any());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_event", "O20251231-009", 2000);

		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

		// ArgumentCaptor를 사용하여 실제로 발행된 이벤트 검증
		ArgumentCaptor<PaymentSuccessEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

		PaymentSuccessEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.getOrderNo()).isEqualTo("O20251231-009");
		assertThat(publishedEvent.getPaymentId()).isEqualTo(paymentId);
		assertThat(publishedEvent.getTotalAmount()).isEqualTo(2000);
		assertThat(publishedEvent.getUserId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("성공: 결제 취소")
	void cancelPayment_success() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-010")
				.status(OrderStatus.PAID)
				.totalAmount(3000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(3000)
				.rawPayload("{\"paymentKey\":\"pay_cancel_key\",\"totalAmount\":3000}")
				.build();

		TossPaymentsCancelResponse cancelResponse = new TossPaymentsCancelResponse();
		cancelResponse.setPaymentKey("pay_cancel_key");
		cancelResponse.setStatus("CANCELED");
		cancelResponse.setTotalAmount(3000);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(payment));
		when(tossPaymentsClient.cancel(eq("pay_cancel_key"), any(TossPaymentsCancelRequest.class)))
				.thenReturn(cancelResponse);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TossPaymentService.TossPaymentCancelResult result =
				tossPaymentService.cancelPayment(orderId, "사용자 요청");

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getOrderId()).isEqualTo(orderId);
		assertThat(result.getCancelAmount()).isEqualTo(3000);
		assertThat(result.getStatus()).isEqualTo("CANCELED");
		assertThat(result.getCancelReason()).isEqualTo("사용자 요청");
	}

	@Test
	@DisplayName("실패: 결제 취소 - 주문을 찾을 수 없음")
	void cancelPayment_orderNotFound() {
		UUID orderId = UUID.randomUUID();
		when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> tossPaymentService.cancelPayment(orderId, "취소 요청"))
				.isInstanceOf(OrderNotFoundException.class);
	}

	@Test
	@DisplayName("실패: 결제 취소 - 결제 내역이 없음")
	void cancelPayment_paymentNotFound() {
		UUID orderId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-011")
				.status(OrderStatus.PAID)
				.totalAmount(1000)
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of());

		assertThatThrownBy(() -> tossPaymentService.cancelPayment(orderId, "취소 요청"))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("실패: 결제 취소 - 결제 상태가 PAID가 아님")
	void cancelPayment_invalidPaymentStatus() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-012")
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(1000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY) // PAID가 아님
				.amount(1000)
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(payment));

		assertThatThrownBy(() -> tossPaymentService.cancelPayment(orderId, "취소 요청"))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("실패: 결제 취소 - rawPayload에서 paymentKey 추출 실패")
	void cancelPayment_paymentKeyExtractionFailed() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-013")
				.status(OrderStatus.PAID)
				.totalAmount(1000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(1000)
				.rawPayload(null) // paymentKey 추출 불가
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(payment));

		assertThatThrownBy(() -> tossPaymentService.cancelPayment(orderId, "취소 요청"))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("성공: extractPaymentKeyFromRawPayload - 정상적인 JSON에서 paymentKey 추출")
	void extractPaymentKeyFromRawPayload_success() {
		// 간접적으로 테스트 - cancelPayment에서 정상적인 JSON 사용
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-014")
				.status(OrderStatus.PAID)
				.totalAmount(1000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(1000)
				.rawPayload("{\"paymentKey\":\"pay_extract_success\",\"totalAmount\":1000}")
				.build();

		TossPaymentsCancelResponse cancelResponse = new TossPaymentsCancelResponse();
		cancelResponse.setPaymentKey("pay_extract_success");
		cancelResponse.setStatus("CANCELED");
		cancelResponse.setTotalAmount(1000);

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(payment));
		when(tossPaymentsClient.cancel(eq("pay_extract_success"), any(TossPaymentsCancelRequest.class)))
				.thenReturn(cancelResponse);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TossPaymentService.TossPaymentCancelResult result =
				tossPaymentService.cancelPayment(orderId, "정상 취소");

		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getStatus()).isEqualTo("CANCELED");
		verify(tossPaymentsClient).cancel(eq("pay_extract_success"), any());
	}

	@Test
	@DisplayName("실패: extractPaymentKeyFromRawPayload - 잘못된 JSON 형식")
	void extractPaymentKeyFromRawPayload_invalidJson() {
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-015")
				.status(OrderStatus.PAID)
				.totalAmount(1000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(1000)
				.rawPayload("invalid-json-format") // 잘못된 JSON
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(payment));

		assertThatThrownBy(() -> tossPaymentService.cancelPayment(orderId, "취소 요청"))
				.isInstanceOf(PaymentException.class);
	}

	private TossPaymentsConfirmResponse confirmResponseWithAmount(Integer amount) {
		TossPaymentsConfirmResponse response = new TossPaymentsConfirmResponse();
		response.setPaymentKey("pay_123");
		response.setOrderId("test-order");
		response.setTotalAmount(amount);
		response.setStatus("DONE");
		response.setApprovedAt("2025-01-15T10:30:00+09:00");
		return response;
	}

	// 추가 테스트: 0% 커버리지 메서드들을 위한 테스트
	@Test
	@DisplayName("실패: validateAmount - null 금액")
	void validateAmount_nullAmount() {
		// validateAmount는 private 메서드이므로 confirmPayment를 통해 간접 테스트
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001004");
		String orderNo = "O20251231-001004";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(5000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_null_amount"))
				.thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(List.of());

		// null 금액으로 confirmPayment 호출
		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_null_amount", orderNo, null))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("실패: validateAmount - 0 이하의 금액")
	void validateAmount_invalidAmount() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001005");
		String orderNo = "O20251231-001005";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(5000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_invalid_amount"))
				.thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(List.of());

		// 0 이하 금액으로 confirmPayment 호출
		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_invalid_amount", orderNo, -1000))
				.isInstanceOf(PaymentException.class);

		assertThatThrownBy(() -> tossPaymentService.confirmPayment("pay_invalid_amount", orderNo, 0))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("실패: ensureStatus - 잘못된 결제 상태 전환")
	void ensureStatus_invalidStatusTransition() {
		// ensureStatus는 private 메서드이므로 특정 시나리오를 통해 간접 테스트
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-016")
				.status(OrderStatus.PAID)
				.totalAmount(2000)
				.build();

		// FAILED 상태의 결제를 PAID로 변경하려고 시도하는 상황 시뮬레이션
		Payment failedPayment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.FAILED)
				.amount(2000)
				.rawPayload("{\"paymentKey\":\"pay_failed_key\",\"totalAmount\":2000}")
				.build();

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(failedPayment));

		// FAILED 상태의 결제를 취소하려고 시도 (ensureStatus 호출됨)
		assertThatThrownBy(() -> tossPaymentService.cancelPayment(orderId, "실패된 결제 취소"))
				.isInstanceOf(PaymentException.class);
	}

	@Test
	@DisplayName("성공: serializePayload(TossPaymentsCancelResponse) - JSON 직렬화")
	void serializePayload_cancelResponse_success() {
		// serializePayload는 private 메서드이므로 cancelPayment를 통해 간접 테스트
		UUID orderId = UUID.randomUUID();
		UUID paymentId = UUID.randomUUID();

		Order order = Order.builder()
				.id(orderId)
				.orderNo("O20251231-017")
				.status(OrderStatus.PAID)
				.totalAmount(3000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.PAID)
				.amount(3000)
				.rawPayload("{\"paymentKey\":\"pay_serialize_test\",\"totalAmount\":3000}")
				.build();

		TossPaymentsCancelResponse cancelResponse = new TossPaymentsCancelResponse();
		cancelResponse.setPaymentKey("pay_serialize_test");
		cancelResponse.setStatus("CANCELED");
		cancelResponse.setTotalAmount(3000);
		cancelResponse.setMethod("카드");

		when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
				.thenReturn(List.of(payment));
		when(tossPaymentsClient.cancel(eq("pay_serialize_test"), any(TossPaymentsCancelRequest.class)))
				.thenReturn(cancelResponse);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
			Payment savedPayment = invocation.getArgument(0);
			// serializePayload가 호출되어 rawPayload가 설정되는지 확인
			assertThat(savedPayment.getRawPayload()).isNotNull();
			assertThat(savedPayment.getRawPayload()).contains("pay_serialize_test");
			assertThat(savedPayment.getRawPayload()).contains("CANCELED");
			return savedPayment;
		});

		TossPaymentService.TossPaymentCancelResult result =
				tossPaymentService.cancelPayment(orderId, "serialize 테스트");

		assertThat(result.getStatus()).isEqualTo("CANCELED");
		verify(paymentRepository).save(any(Payment.class));
	}

	@Test
	@DisplayName("성공: publishPaymentSuccessEvent - 이벤트 발행 성공 (확장)")
	void publishPaymentSuccessEvent_withOrderItems() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001006");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004006");
		String orderNo = "O20251231-001006";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.orderType(OrderType.PURCHASE)
				.customerId(12345L)
				.totalAmount(5000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(5000)
				.build();

		// 주문 항목 데이터
		OrderItem orderItem = OrderItem.builder()
				.id(UUID.randomUUID())
				.orderId(orderId)
				.qty(2)
				.unitPrice(2500)
				.lineAmount(5000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_event_test"))
				.thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentCreationResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(5000)
						.build();
		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(5000), any(String.class)))
				.thenReturn(paymentCreationResult);
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(confirmResponseWithAmount(5000));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		// 주문 항목 조회 설정
		when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of(orderItem));

		// 이벤트 발행 설정
		doNothing().when(eventPublisher).publishEvent(any());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_event_test", orderNo, 5000);

		assertThat(result.getPaymentId()).isEqualTo(paymentId);

		// ArgumentCaptor를 사용하여 실제로 발행된 이벤트 검증
		ArgumentCaptor<PaymentSuccessEvent> eventCaptor = ArgumentCaptor.forClass(PaymentSuccessEvent.class);
		verify(eventPublisher, times(1)).publishEvent(eventCaptor.capture());

		PaymentSuccessEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.getOrderNo()).isEqualTo(orderNo);
		assertThat(publishedEvent.getPaymentId()).isEqualTo(paymentId);
		assertThat(publishedEvent.getTotalAmount()).isEqualTo(5000);
		assertThat(publishedEvent.getUserId()).isEqualTo(12345L);
	}

	@Test
	@DisplayName("성공: publishPaymentSuccessEvent - 이벤트 발행 실패해도 결제는 성공")
	void publishPaymentSuccessEvent_eventFailure() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001007");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004007");
		String orderNo = "O20251231-001007";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.customerId(12346L)
				.totalAmount(6000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(6000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_event_fail_test"))
				.thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentCreationResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(6000)
						.build();
		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(6000), any(String.class)))
				.thenReturn(paymentCreationResult);
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
		when(tossPaymentsClient.confirm(any())).thenReturn(confirmResponseWithAmount(6000));
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		// 주문 항목 조회에서 예외 발생
		when(orderItemRepository.findByOrderId(orderId)).thenThrow(new RuntimeException("DB 연결 실패"));

		// 이벤트 발행은 호출되지 않아야 함 (주문 항목 조회 실패로 인해)

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_event_fail_test", orderNo, 6000);

		// 이벤트 발행이 실패해도 결제는 성공해야 함
		assertThat(result.getPaymentId()).isEqualTo(paymentId);
		assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

		// 이벤트가 발행되지 않았는지 확인 (예외로 인해)
		verify(eventPublisher, never()).publishEvent(any());
	}

	@Test
	@DisplayName("성공: parseApprovedAt - null과 빈 문자열 처리")
	void parseApprovedAt_nullAndEmpty() {
		// parseApprovedAt는 private 메서드이므로 confirmPayment를 통해 간접 테스트
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001008");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004008");
		String orderNo = "O20251231-001008";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(7000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(7000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_null_approved_at"))
				.thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentCreationResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(7000)
						.build();
		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(7000), any(String.class)))
				.thenReturn(paymentCreationResult);
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

		// null approvedAt을 가진 응답
		TossPaymentsConfirmResponse responseWithNullApprovedAt = new TossPaymentsConfirmResponse();
		responseWithNullApprovedAt.setPaymentKey("pay_null_approved_at");
		responseWithNullApprovedAt.setOrderId(orderNo);
		responseWithNullApprovedAt.setTotalAmount(7000);
		responseWithNullApprovedAt.setStatus("DONE");
		responseWithNullApprovedAt.setApprovedAt(null); // null approvedAt

		when(tossPaymentsClient.confirm(any())).thenReturn(responseWithNullApprovedAt);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_null_approved_at", orderNo, 7000);

		// null approvedAt이어도 현재 시간으로 설정되어야 함
		assertThat(result.getApprovedAt()).isNotNull();
		assertThat(result.getApprovedAt()).isBeforeOrEqualTo(LocalDateTime.now());
	}

	@Test
	@DisplayName("성공: parseApprovedAt - 잘못된 날짜 형식 처리")
	void parseApprovedAt_invalidDateFormat() {
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001009");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004009");
		String orderNo = "O20251231-001009";

		Order order = Order.builder()
				.id(orderId)
				.orderNo(orderNo)
				.status(OrderStatus.PAYMENT_PENDING)
				.totalAmount(8000)
				.build();

		Payment payment = Payment.builder()
				.id(paymentId)
				.orderId(orderId)
				.status(PaymentStatus.READY)
				.amount(8000)
				.build();

		when(orderRepository.findByOrderNo(orderNo)).thenReturn(Optional.of(order));
		when(paymentRepository.findByPaymentKeyInRawPayload("pay_invalid_date"))
				.thenReturn(List.of());
		when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(any(UUID.class)))
				.thenReturn(List.of());

		PaymentCommandService.PaymentCreationResult paymentCreationResult =
				PaymentCommandService.PaymentCreationResult.builder()
						.paymentId(paymentId)
						.paymentStatus(PaymentStatus.READY)
						.orderStatus(OrderStatus.PAYMENT_PENDING)
						.amount(8000)
						.build();
		when(paymentCommandService.createPayment(eq(orderId), eq("CARD"), eq(8000), any(String.class)))
				.thenReturn(paymentCreationResult);
		when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

		// 잘못된 날짜 형식을 가진 응답
		TossPaymentsConfirmResponse responseWithInvalidDate = new TossPaymentsConfirmResponse();
		responseWithInvalidDate.setPaymentKey("pay_invalid_date");
		responseWithInvalidDate.setOrderId(orderNo);
		responseWithInvalidDate.setTotalAmount(8000);
		responseWithInvalidDate.setStatus("DONE");
		responseWithInvalidDate.setApprovedAt("invalid-date-format"); // 잘못된 형식

		when(tossPaymentsClient.confirm(any())).thenReturn(responseWithInvalidDate);
		when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(orderCommandService.updateStatus(eq(orderId), eq(OrderStatus.PAID.name()), any()))
				.thenReturn(Order.builder().id(orderId).status(OrderStatus.PAID).build());

		TossPaymentService.TossPaymentConfirmResult result =
				tossPaymentService.confirmPayment("pay_invalid_date", orderNo, 8000);

		// 잘못된 날짜 형식이어도 현재 시간으로 설정되어야 함
		assertThat(result.getApprovedAt()).isNotNull();
		assertThat(result.getApprovedAt()).isBeforeOrEqualTo(LocalDateTime.now());
	}
}
