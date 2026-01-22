package com.popcorn.demo.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.payment.entity.Payment;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.entity.PaymentStatus;
import com.popcorn.demo.domain.payment.exception.PaymentException;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import com.popcorn.demo.domain.qr.dto.response.QrCodeResponse;
import com.popcorn.demo.domain.qr.exception.QrException;
import com.popcorn.demo.domain.qr.service.QrCodeService;

class PaymentQueryServiceTest {

    @Test
    void getPaymentReturnsQrWhenPaidReservation() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PAID)
                .amount(15000)
                .approvedAt(LocalDateTime.now())
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
        QrCodeResponse qrCodeResponse = QrCodeResponse.builder()
                .orderId(orderId)
                .qrCode("QR-123")
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();
        when(qrCodeService.get(orderId)).thenReturn(qrCodeResponse);

        PaymentQueryService.PaymentDetailResult result = service.getPayment(paymentId);

        assertThat(result.getQrAvailable()).isTrue();
        assertThat(result.getQrCode()).isEqualTo("QR-123");
    }

    @Test
    void getPaymentReturnsNoQrWhenNotPaid() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.READY)
                .amount(15000)
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentQueryService.PaymentDetailResult result = service.getPayment(paymentId);

        assertThat(result.getQrAvailable()).isFalse();
        assertThat(result.getQrCode()).isNull();
    }

    @Test
    void getPaymentThrowsWhenDeleted() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.READY)
                .amount(15000)
                .build();
        payment.setDeletedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.getPayment(paymentId))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void getPaymentsByOrderHandlesMissingQr() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PAID)
                .amount(9000)
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of(payment));
        when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
        when(qrCodeService.get(orderId)).thenThrow(QrException.qrNotFound());

        List<PaymentQueryService.PaymentDetailResult> results = service.getPaymentsByOrder(orderId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQrAvailable()).isFalse();
    }

    @Test
    void getPaymentThrowsWhenNotFound() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment(paymentId))
                .isInstanceOf(PaymentException.class);
    }

    @Test
    void getPaymentReturnsNoQrForNonReservationOrder() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PAID)
                .amount(12000)
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(false);

        PaymentQueryService.PaymentDetailResult result = service.getPayment(paymentId);

        assertThat(result.getQrAvailable()).isFalse();
        assertThat(result.getQrCode()).isNull();
    }

    @Test
    void getPaymentHandlesQrServiceException() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.PAID)
                .amount(8000)
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(orderItemRepository.existsByOrderIdAndSessionOptionIdIsNotNull(orderId)).thenReturn(true);
        when(qrCodeService.get(orderId)).thenThrow(new RuntimeException("Network error"));

        PaymentQueryService.PaymentDetailResult result = service.getPayment(paymentId);

        assertThat(result.getQrAvailable()).isFalse();
        assertThat(result.getQrCode()).isNull();
    }

    @Test
    void getPaymentsByOrderReturnsEmptyList() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID orderId = UUID.randomUUID();
        when(paymentRepository.findAllByOrderIdAndDeletedAtIsNullOrderByCreatedAtDesc(orderId))
                .thenReturn(List.of());

        List<PaymentQueryService.PaymentDetailResult> results = service.getPaymentsByOrder(orderId);

        assertThat(results).isEmpty();
    }

    @Test
    void getPaymentWithFailedStatus() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.FAILED)
                .amount(25000)
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentQueryService.PaymentDetailResult result = service.getPayment(paymentId);

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getQrAvailable()).isFalse();
        assertThat(result.getQrCode()).isNull();
    }

    @Test
    void getPaymentWithCancelledStatus() {
        JpaPaymentRepository paymentRepository = Mockito.mock(JpaPaymentRepository.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        PaymentQueryService service = new PaymentQueryService(paymentRepository, orderItemRepository, qrCodeService);

        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .orderId(UUID.randomUUID())
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.CANCELLED)
                .amount(30000)
                .build();
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        PaymentQueryService.PaymentDetailResult result = service.getPayment(paymentId);

        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(result.getQrAvailable()).isFalse();
        assertThat(result.getQrCode()).isNull();
    }
}
