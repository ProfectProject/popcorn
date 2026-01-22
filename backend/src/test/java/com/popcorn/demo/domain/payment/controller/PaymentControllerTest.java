package com.popcorn.demo.domain.payment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.payment.dto.request.PaymentStatusUpdateRequest;
import com.popcorn.demo.domain.payment.dto.response.PaymentDetailResponse;
import com.popcorn.demo.domain.payment.dto.response.PaymentListResponse;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.service.PaymentCommandService;
import com.popcorn.demo.domain.payment.service.PaymentQueryService;
import com.popcorn.demo.domain.payment.service.PaymentTokenService;
import com.popcorn.demo.domain.payment.toss.TossPaymentsProperties;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.payment.dto.response.PaymentCreateResponse;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.exception.PaymentException;

class PaymentControllerTest {

    @Mock
    private PaymentCommandService paymentCommandService;

    @Mock
    private PaymentQueryService paymentQueryService;

    @Mock
    private PaymentTokenService paymentTokenService;

    @Mock
    private TossPaymentsProperties tossPaymentsProperties;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private JpaPaymentRepository paymentRepository;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new PaymentController(
                paymentCommandService,
                paymentQueryService,
                paymentTokenService,
                tossPaymentsProperties,
                orderRepository,
                paymentRepository
        );
    }

    @Test
    void getPaymentsByOrder_WithSinglePayment_ReturnsPaymentList() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(50000)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentQueryService.getPaymentsByOrder(orderId)).thenReturn(List.of(result));

        // when
        ResponseEntity<BaseResponse<PaymentListResponse>> response = controller.getPaymentsByOrder(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getCount()).isEqualTo(1);
        assertThat(response.getBody().getData().getItems()).hasSize(1);
        assertThat(response.getBody().getData().getItems().get(0).getPaymentId()).isEqualTo(paymentId);
        verify(paymentQueryService).getPaymentsByOrder(orderId);
    }

    @Test
    void getPaymentsByOrder_EmptyList_ReturnsEmptyResponse() {
        // given
        UUID orderId = UUID.randomUUID();
        when(paymentQueryService.getPaymentsByOrder(orderId)).thenReturn(List.of());

        // when
        ResponseEntity<BaseResponse<PaymentListResponse>> response = controller.getPaymentsByOrder(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getCount()).isEqualTo(0);
        assertThat(response.getBody().getData().getItems()).isEmpty();
        verify(paymentQueryService).getPaymentsByOrder(orderId);
    }

    @Test
    void getPaymentsByOrder_WithMultiplePayments_ReturnsOrderedList() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId1 = UUID.randomUUID();
        UUID paymentId2 = UUID.randomUUID();

        PaymentQueryService.PaymentDetailResult result1 = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId1)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(30000)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        PaymentQueryService.PaymentDetailResult result2 = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId2)
                .orderId(orderId)
                .method(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.FAILED)
                .amount(20000)
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentQueryService.getPaymentsByOrder(orderId)).thenReturn(List.of(result1, result2));

        // when
        ResponseEntity<BaseResponse<PaymentListResponse>> response = controller.getPaymentsByOrder(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getCount()).isEqualTo(2);
        assertThat(response.getBody().getData().getItems()).hasSize(2);
        verify(paymentQueryService).getPaymentsByOrder(orderId);
    }

    @Test
    void getPayment_ReturnsPaymentDetail() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.READY)
                .amount(30000)
                .approvedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(OrderStatus.REQUESTED)
                .qrAvailable(false)
                .qrCode(null)
                .qrExpiresAt(null)
                .build();

        when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response = controller.getPayment(paymentId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getBody().getData().getMethod()).isEqualTo("TRANSFER");
        assertThat(response.getBody().getData().getStatus()).isEqualTo("READY");
        assertThat(response.getBody().getData().getAmount()).isEqualTo(30000);
        verify(paymentQueryService).getPayment(paymentId);
    }

    @Test
    void getPayment_WithNullValues_HandlesGracefully() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(null) // null method
                .paymentStatus(null) // null status
                .amount(25000)
                .approvedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(null) // null order status
                .qrAvailable(false)
                .qrCode(null)
                .qrExpiresAt(null)
                .build();

        when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response = controller.getPayment(paymentId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getBody().getData().getMethod()).isNull();
        assertThat(response.getBody().getData().getStatus()).isNull();
        assertThat(response.getBody().getData().getOrderStatus()).isNull();
        verify(paymentQueryService).getPayment(paymentId);
    }

    @Test
    void updatePaymentStatus_ToPaid_ReturnsUpdatedPayment() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("PAID");

        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(50000)
                .approvedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(OrderStatus.COMPLETED)
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "PAID")).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response =
                controller.updatePaymentStatus(paymentId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("PAID");
        assertThat(response.getBody().getData().getQrAvailable()).isFalse(); // Command는 QR 정보 없음
        verify(paymentCommandService).updatePaymentStatus(paymentId, "PAID");
    }

    @Test
    void updatePaymentStatus_ToFailed_ReturnsUpdatedPayment() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("FAILED");

        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.EASY_PAY)
                .paymentStatus(PaymentStatus.FAILED)
                .amount(75000)
                .approvedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "FAILED")).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response =
                controller.updatePaymentStatus(paymentId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("FAILED");
        verify(paymentCommandService).updatePaymentStatus(paymentId, "FAILED");
    }

    @Test
    void updatePaymentStatus_ToCancelled_ReturnsUpdatedPayment() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("CANCELLED");

        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.CANCELLED)
                .amount(40000)
                .approvedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(OrderStatus.CANCELLED)
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "CANCELLED")).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response =
                controller.updatePaymentStatus(paymentId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getPaymentId()).isEqualTo(paymentId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("CANCELLED");
        verify(paymentCommandService).updatePaymentStatus(paymentId, "CANCELLED");
    }

    @Test
    void updatePaymentStatus_WithNullValues_HandlesGracefully() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("READY");

        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(null) // null method
                .paymentStatus(null) // null status
                .amount(35000)
                .approvedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(null) // null order status
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "READY")).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response =
                controller.updatePaymentStatus(paymentId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getMethod()).isNull();
        assertThat(response.getBody().getData().getStatus()).isNull();
        assertThat(response.getBody().getData().getOrderStatus()).isNull();
        verify(paymentCommandService).updatePaymentStatus(paymentId, "READY");
    }

    @Test
    void deletePayment_ReturnsNoContent() {
        // given
        UUID paymentId = UUID.randomUUID();

        // when
        ResponseEntity<Void> response = controller.deletePayment(paymentId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(paymentCommandService).deletePayment(paymentId);
    }

    @Test
    void getPayment_CallsServiceCorrectly() {
        // given
        UUID paymentId = UUID.randomUUID();
        PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(UUID.randomUUID())
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(100000)
                .build();

        when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

        // when
        controller.getPayment(paymentId);

        // then
        verify(paymentQueryService).getPayment(paymentId);
    }

    @Test
    void updatePaymentStatus_CallsServiceCorrectly() {
        // given
        UUID paymentId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("PAID");
        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(UUID.randomUUID())
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(100000)
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "PAID")).thenReturn(result);

        // when
        controller.updatePaymentStatus(paymentId, request);

        // then
        verify(paymentCommandService).updatePaymentStatus(paymentId, "PAID");
    }

    @Test
    void deletePayment_CallsServiceCorrectly() {
        // given
        UUID paymentId = UUID.randomUUID();

        // when
        controller.deletePayment(paymentId);

        // then
        verify(paymentCommandService).deletePayment(paymentId);
    }

    @Test
    void getPaymentsByOrder_WithCompletePaymentData_ConvertsToListItems() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId1 = UUID.randomUUID();
        UUID paymentId2 = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PaymentQueryService.PaymentDetailResult result1 = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId1)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(50000)
                .approvedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .orderStatus(OrderStatus.COMPLETED)
                .qrAvailable(true)
                .qrCode("QR123")
                .qrExpiresAt(now.plusHours(1))
                .build();

        PaymentQueryService.PaymentDetailResult result2 = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId2)
                .orderId(orderId)
                .method(PaymentMethod.EASY_PAY)
                .paymentStatus(PaymentStatus.READY)
                .amount(25000)
                .approvedAt(null)
                .createdAt(now.minusMinutes(10))
                .updatedAt(now)
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .qrAvailable(false)
                .qrCode(null)
                .qrExpiresAt(null)
                .build();

        when(paymentQueryService.getPaymentsByOrder(orderId)).thenReturn(List.of(result1, result2));

        // when
        ResponseEntity<BaseResponse<PaymentListResponse>> response = controller.getPaymentsByOrder(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        PaymentListResponse data = response.getBody().getData();
        assertThat(data.getCount()).isEqualTo(2);
        assertThat(data.getItems()).hasSize(2);

        // Test first item
        PaymentListResponse.Item item1 = data.getItems().get(0);
        assertThat(item1.getPaymentId()).isEqualTo(paymentId1);
        assertThat(item1.getMethod()).isEqualTo("CARD");
        assertThat(item1.getStatus()).isEqualTo("PAID");
        assertThat(item1.getAmount()).isEqualTo(50000);

        // Test second item
        PaymentListResponse.Item item2 = data.getItems().get(1);
        assertThat(item2.getPaymentId()).isEqualTo(paymentId2);
        assertThat(item2.getMethod()).isEqualTo("EASY_PAY");
        assertThat(item2.getStatus()).isEqualTo("READY");
        assertThat(item2.getAmount()).isEqualTo(25000);
    }

    @Test
    void getPayment_WithCompleteData_ConvertsToDetailResponse() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.PAID)
                .amount(75000)
                .approvedAt(now)
                .createdAt(now.minusHours(1))
                .updatedAt(now)
                .orderStatus(OrderStatus.COMPLETED)
                .qrAvailable(true)
                .qrCode("QR456")
                .qrExpiresAt(now.plusHours(2))
                .build();

        when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response = controller.getPayment(paymentId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        PaymentDetailResponse data = response.getBody().getData();
        assertThat(data.getPaymentId()).isEqualTo(paymentId);
        assertThat(data.getOrderId()).isEqualTo(orderId);
        assertThat(data.getMethod()).isEqualTo("TRANSFER");
        assertThat(data.getStatus()).isEqualTo("PAID");
        assertThat(data.getAmount()).isEqualTo(75000);
        assertThat(data.getApprovedAt()).isEqualTo(now);
        assertThat(data.getCreatedAt()).isEqualTo(now.minusHours(1));
        assertThat(data.getUpdatedAt()).isEqualTo(now);
        assertThat(data.getOrderStatus()).isEqualTo("COMPLETED");
        assertThat(data.getQrAvailable()).isTrue();
        assertThat(data.getQrCode()).isEqualTo("QR456");
        assertThat(data.getQrExpiresAt()).isEqualTo(now.plusHours(2));
    }

    @Test
    void updatePaymentStatus_WithCompleteData_ConvertsToCommandDetailResponse() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("PAID");

        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .paymentStatus(PaymentStatus.PAID)
                .amount(100000)
                .approvedAt(now)
                .createdAt(now.minusHours(2))
                .updatedAt(now)
                .orderStatus(OrderStatus.COMPLETED)
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "PAID")).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response =
                controller.updatePaymentStatus(paymentId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        PaymentDetailResponse data = response.getBody().getData();
        assertThat(data.getPaymentId()).isEqualTo(paymentId);
        assertThat(data.getOrderId()).isEqualTo(orderId);
        assertThat(data.getMethod()).isEqualTo("CARD");
        assertThat(data.getStatus()).isEqualTo("PAID");
        assertThat(data.getAmount()).isEqualTo(100000);
        assertThat(data.getApprovedAt()).isEqualTo(now);
        assertThat(data.getCreatedAt()).isEqualTo(now.minusHours(2));
        assertThat(data.getUpdatedAt()).isEqualTo(now);
        assertThat(data.getOrderStatus()).isEqualTo("COMPLETED");
        // Command service responses don't include QR info
        assertThat(data.getQrAvailable()).isFalse();
        assertThat(data.getQrCode()).isNull();
        assertThat(data.getQrExpiresAt()).isNull();
    }

    @Test
    void updatePaymentStatus_ToReady_ReturnsUpdatedPayment() {
        // given
        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        PaymentStatusUpdateRequest request = new PaymentStatusUpdateRequest("READY");

        PaymentCommandService.PaymentDetailResult result = PaymentCommandService.PaymentDetailResult.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.TRANSFER)
                .paymentStatus(PaymentStatus.READY)
                .amount(30000)
                .approvedAt(null)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .build();

        when(paymentCommandService.updatePaymentStatus(paymentId, "READY")).thenReturn(result);

        // when
        ResponseEntity<BaseResponse<PaymentDetailResponse>> response =
                controller.updatePaymentStatus(paymentId, request);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("READY");
        assertThat(response.getBody().getData().getOrderStatus()).isEqualTo("PAYMENT_PENDING");
        verify(paymentCommandService).updatePaymentStatus(paymentId, "READY");
    }

    @Test
    void getPayment_WithAllOrderStatuses_ConvertsCorrectly() {
        // Test various order statuses
        UUID paymentId = UUID.randomUUID();

        for (OrderStatus orderStatus : OrderStatus.values()) {
            PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                    .paymentId(paymentId)
                    .orderId(UUID.randomUUID())
                    .method(PaymentMethod.CARD)
                    .paymentStatus(PaymentStatus.READY)
                    .amount(10000)
                    .orderStatus(orderStatus)
                    .build();

            when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

            // when
            ResponseEntity<BaseResponse<PaymentDetailResponse>> response = controller.getPayment(paymentId);

            // then
            assertThat(response.getBody().getData().getOrderStatus()).isEqualTo(orderStatus.name());
        }
    }

    @Test
    void getPayment_WithAllPaymentMethods_ConvertsCorrectly() {
        // Test various payment methods
        UUID paymentId = UUID.randomUUID();

        for (PaymentMethod method : PaymentMethod.values()) {
            PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                    .paymentId(paymentId)
                    .orderId(UUID.randomUUID())
                    .method(method)
                    .paymentStatus(PaymentStatus.READY)
                    .amount(10000)
                    .orderStatus(OrderStatus.REQUESTED)
                    .build();

            when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

            // when
            ResponseEntity<BaseResponse<PaymentDetailResponse>> response = controller.getPayment(paymentId);

            // then
            assertThat(response.getBody().getData().getMethod()).isEqualTo(method.name());
        }
    }

    @Test
    void getPayment_WithAllPaymentStatuses_ConvertsCorrectly() {
        // Test various payment statuses
        UUID paymentId = UUID.randomUUID();

        for (PaymentStatus status : PaymentStatus.values()) {
            PaymentQueryService.PaymentDetailResult result = PaymentQueryService.PaymentDetailResult.builder()
                    .paymentId(paymentId)
                    .orderId(UUID.randomUUID())
                    .method(PaymentMethod.CARD)
                    .paymentStatus(status)
                    .amount(10000)
                    .orderStatus(OrderStatus.REQUESTED)
                    .build();

            when(paymentQueryService.getPayment(paymentId)).thenReturn(result);

            // when
            ResponseEntity<BaseResponse<PaymentDetailResponse>> response = controller.getPayment(paymentId);

            // then
            assertThat(response.getBody().getData().getStatus()).isEqualTo(status.name());
        }
    }

    @Test
    void retryPayment_withPaidLatest_throwsPaymentException() {
        // given
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNo("O20250101-000001")
                .customerId(10L)
                .status(OrderStatus.PAYMENT_PENDING)
                .totalAmount(10000)
                .build();
        Payment latest = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .status(PaymentStatus.PAID)
                .amount(10000)
                .method(PaymentMethod.CARD)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.of(order));
        when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of(latest));

        // when / then
        assertThatThrownBy(() -> controller.retryPayment(orderId))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void retryPayment_withReadyLatest_returnsExistingPaymentId() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNo("O20250101-000002")
                .customerId(42L)
                .status(OrderStatus.PAYMENT_PENDING)
                .totalAmount(25000)
                .build();
        Payment latest = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .status(PaymentStatus.READY)
                .amount(25000)
                .method(PaymentMethod.TRANSFER)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.of(order));
        when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of(latest));
        when(paymentTokenService.createPaymentToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn("token123");
        when(tossPaymentsProperties.getSuccessUrl()).thenReturn("https://success.url");
        when(tossPaymentsProperties.getFailUrl()).thenReturn("https://fail.url");
        when(tossPaymentsProperties.getClientKey()).thenReturn("client-key");

        // when
        ResponseEntity<BaseResponse<PaymentCreateResponse>> response = controller.retryPayment(orderId);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        PaymentCreateResponse data = response.getBody().getData();
        assertThat(data.getPaymentId()).isEqualTo(paymentId);
        assertThat(data.getOrderId()).isEqualTo(orderId);
        assertThat(data.getOrderNo()).isEqualTo("O20250101-000002");
        assertThat(data.getAmount()).isEqualTo(25000);
        assertThat(data.getCustomerKey()).isEqualTo("user_42@popcorn.demo");
        assertThat(data.getSuccessUrl()).isEqualTo("https://success.url");
        assertThat(data.getFailUrl()).isEqualTo("https://fail.url");
        assertThat(data.getPaymentToken()).isEqualTo("token123");
        assertThat(data.getClientKey()).isEqualTo("client-key");
        assertThat(data.getReadyForPayment()).isTrue();
    }

    @Test
    void retryPayment_withoutLatestPayment_usesGuestCustomerKey() {
        // given
        UUID orderId = UUID.randomUUID();
        Order order = Order.builder()
                .id(orderId)
                .orderNo("O20250101-000003")
                .customerId(null)
                .status(OrderStatus.PAYMENT_PENDING)
                .totalAmount(15000)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(java.util.Optional.of(order));
        when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of());
        when(paymentTokenService.createPaymentToken(org.mockito.ArgumentMatchers.any()))
                .thenReturn("token456");
        when(tossPaymentsProperties.getSuccessUrl()).thenReturn("https://success.local");
        when(tossPaymentsProperties.getFailUrl()).thenReturn("https://fail.local");
        when(tossPaymentsProperties.getClientKey()).thenReturn("client-key-2");

        // when
        ResponseEntity<BaseResponse<PaymentCreateResponse>> response = controller.retryPayment(orderId);

        // then
        PaymentCreateResponse data = response.getBody().getData();
        assertThat(data.getPaymentId()).isNull();
        assertThat(data.getCustomerKey()).startsWith("guest_");
        assertThat(data.getPaymentToken()).isEqualTo("token456");
        assertThat(data.getReadyForPayment()).isTrue();
    }
}
