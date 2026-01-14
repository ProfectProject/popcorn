package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.exception.OrderNotFoundException;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;

class OrderPaymentRedirectControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private JpaPaymentRepository paymentRepository;

    @Mock
    private TossPaymentsProperties tossPaymentsProperties;

    private OrderPaymentRedirectController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new OrderPaymentRedirectController(orderRepository, paymentRepository, tossPaymentsProperties);
    }

    @Test
    void redirectToPayment_WithValidOrderAndPayment_ReturnsRedirect() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Long customerId = 123L;
        String orderNo = "ORDER-001";
        Integer amount = 50000;

        Order order = Order.builder()
                .id(orderId)
                .orderNo(orderNo)
                .customerId(customerId)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .amount(amount)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.READY)
                .build();

        String checkoutUrl = "https://checkout.toss.im";
        String successUrl = "https://example.com/success";
        String failUrl = "https://example.com/fail";

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentsProperties.getCheckoutUrl()).thenReturn(checkoutUrl);
        when(tossPaymentsProperties.getSuccessUrl()).thenReturn(successUrl);
        when(tossPaymentsProperties.getFailUrl()).thenReturn(failUrl);

        // when
        ResponseEntity<Void> response = controller.redirectToPayment(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();

        String locationStr = location.toString();
        assertThat(locationStr).contains(checkoutUrl);
        assertThat(locationStr).contains("orderNo=" + orderNo);
        assertThat(locationStr).contains("amount=" + amount);
        assertThat(locationStr).contains("customerKey=" + customerId.toString());
        assertThat(locationStr).contains("paymentId=" + paymentId.toString());
        assertThat(locationStr).contains("successUrl=" + successUrl);
        assertThat(locationStr).contains("failUrl=" + failUrl);

        verify(orderRepository).findById(orderId);
        verify(paymentRepository).findByOrderId(orderId);
    }

    @Test
    void redirectToPayment_WithGuestCustomer_UsesGuestCustomerKey() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Long customerId = null; // guest customer
        String orderNo = "ORDER-002";

        Order order = Order.builder()
                .id(orderId)
                .orderNo(orderNo)
                .customerId(customerId)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .amount(30000)
                .method(PaymentMethod.TRANSFER)
                .status(PaymentStatus.READY)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentsProperties.getCheckoutUrl()).thenReturn("https://checkout.toss.im");
        when(tossPaymentsProperties.getSuccessUrl()).thenReturn("https://success.com");
        when(tossPaymentsProperties.getFailUrl()).thenReturn("https://fail.com");

        // when
        ResponseEntity<Void> response = controller.redirectToPayment(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).contains("customerKey=guest");
    }

    @Test
    void redirectToPayment_WithNonExistentOrder_ThrowsOrderNotFoundException() {
        // given
        UUID orderId = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> controller.redirectToPayment(orderId))
                .isInstanceOf(OrderNotFoundException.class);

        verify(orderRepository).findById(orderId);
    }

    @Test
    void redirectToPayment_WithNonExistentPayment_ThrowsPaymentException() {
        // given
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .orderNo("ORDER-003")
                .customerId(456L)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> controller.redirectToPayment(orderId))
                .isInstanceOf(PaymentException.class);

        verify(orderRepository).findById(orderId);
        verify(paymentRepository).findByOrderId(orderId);
    }

    @Test
    void redirectToPayment_WithDeletedPayment_ThrowsPaymentException() {
        // given
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .orderNo("ORDER-004")
                .customerId(789L)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        Payment deletedPayment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .amount(40000)
                .method(PaymentMethod.EASY_PAY)
                .status(PaymentStatus.CANCELLED)
                .build();
        deletedPayment.setDeletedAt(LocalDateTime.now()); // 삭제된 결제

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(deletedPayment));

        // when & then
        assertThatThrownBy(() -> controller.redirectToPayment(orderId))
                .isInstanceOf(PaymentException.class);

        verify(orderRepository).findById(orderId);
        verify(paymentRepository).findByOrderId(orderId);
    }

    @Test
    void redirectToPayment_WithLargeAmount_HandlesCorrectly() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Integer largeAmount = 999999999;

        Order order = Order.builder()
                .id(orderId)
                .orderNo("ORDER-LARGE")
                .customerId(999L)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .amount(largeAmount)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.READY)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentsProperties.getCheckoutUrl()).thenReturn("https://checkout.toss.im");
        when(tossPaymentsProperties.getSuccessUrl()).thenReturn("https://success.com");
        when(tossPaymentsProperties.getFailUrl()).thenReturn("https://fail.com");

        // when
        ResponseEntity<Void> response = controller.redirectToPayment(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.toString()).contains("amount=" + largeAmount);
    }

    @Test
    void redirectToPayment_WithSpecialCharactersInOrderNo_EncodesCorrectly() {
        // given
        UUID orderId = UUID.randomUUID();
        String orderNoWithSpecialChars = "ORDER-Korean-123";

        Order order = Order.builder()
                .id(orderId)
                .orderNo(orderNoWithSpecialChars)
                .customerId(555L)
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .amount(25000)
                .method(PaymentMethod.TRANSFER)
                .status(PaymentStatus.READY)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(orderId)).thenReturn(Optional.of(payment));
        when(tossPaymentsProperties.getCheckoutUrl()).thenReturn("https://checkout.toss.im");
        when(tossPaymentsProperties.getSuccessUrl()).thenReturn("https://success.com");
        when(tossPaymentsProperties.getFailUrl()).thenReturn("https://fail.com");

        // when
        ResponseEntity<Void> response = controller.redirectToPayment(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(302);
        URI location = response.getHeaders().getLocation();
        assertThat(location).isNotNull();
        // URL should be properly encoded
        assertThat(location.toString()).contains("orderNo=");
    }
}