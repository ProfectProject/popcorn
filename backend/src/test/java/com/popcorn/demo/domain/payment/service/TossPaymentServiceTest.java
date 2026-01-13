package com.popcorn.demo.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.payment.toss.TossPaymentsClient;
import com.popcorn.demo.domain.payment.toss.TossPaymentsConfirmResponse;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
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
				eventPublisher);
	}

	@Test
	@DisplayName("성공: 토스 결제 승인 처리")
	void confirmPayment_success() {
		String orderNo = "O20251231-001003";
		UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000001003");
		UUID paymentId = UUID.fromString("00000000-0000-0000-0000-000000004003");

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
		when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
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
}
