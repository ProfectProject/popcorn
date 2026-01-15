package com.popcorn.demo.domain.payment.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.popcorn.demo.domain.inventory.service.InventoryService;
import com.popcorn.demo.domain.order.entity.OrderItem;
import com.popcorn.demo.domain.order.entity.OrderType;
import com.popcorn.demo.domain.order.repository.jpa.JpaOrderItemRepository;
import com.popcorn.demo.domain.payment.entity.PaymentMethod;
import com.popcorn.demo.domain.payment.service.PaymentCancelFailureService;
import com.popcorn.demo.domain.qr.service.QrCodeService;

class PaymentEventHandlerTest {

    @Test
    void handleInventoryDeductionSkipsEmptyItems() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-1")
                .orderType("RESERVATION")
                .orderItems(List.of())
                .build();

        handler.handleInventoryDeduction(event);

        verify(inventoryService, never()).deductInventoryForOrder(Mockito.any(), Mockito.any());
    }

    @Test
    void handleInventoryDeductionProcessesKnownTypes() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        List<OrderItem> items = List.of(OrderItem.builder().id(UUID.randomUUID()).qty(1).build());

        PaymentSuccessEvent reservation = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo("O-2")
                .orderType("RESERVATION")
                .orderItems(items)
                .build();
        handler.handleInventoryDeduction(reservation);

        PaymentSuccessEvent purchase = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo("O-3")
                .orderType("PURCHASE")
                .orderItems(items)
                .build();
        handler.handleInventoryDeduction(purchase);

        verify(inventoryService, times(2)).deductInventoryForOrder(orderId, items);
    }

    @Test
    void handleQrGenerationOnlyForReservation() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        PaymentSuccessEvent purchase = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo("O-4")
                .orderType("PURCHASE")
                .build();
        handler.handleQrGeneration(purchase);

        PaymentSuccessEvent reservation = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo("O-5")
                .orderType("RESERVATION")
                .build();
        handler.handleQrGeneration(reservation);

        verify(qrCodeService).issue(orderId);
    }

    @Test
    void handlePaymentApprovedProcessesInventoryAndQr() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        List<OrderItem> items = List.of(OrderItem.builder().id(UUID.randomUUID()).qty(2).build());
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(items);

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                this,
                UUID.randomUUID(),
                orderId,
                PaymentMethod.CARD,
                10000,
                OrderType.RESERVATION,
                1L,
                LocalDateTime.now());

        handler.handlePaymentApproved(event);

        verify(inventoryService).deductInventoryForOrder(orderId, items);
        verify(qrCodeService).issue(orderId);
    }

    @Test
    void handlePaymentCancelledRestoresInventory() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        List<OrderItem> items = List.of(OrderItem.builder().id(UUID.randomUUID()).qty(1).build());
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(items);

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                this,
                UUID.randomUUID(),
                orderId,
                PaymentMethod.CARD,
                10000,
                OrderType.PURCHASE,
                1L,
                LocalDateTime.now());

        handler.handlePaymentCancelled(event);

        verify(inventoryService).restoreInventoryForOrder(orderId, items);
    }

    @Test
    void handlePaymentCreatedLogsInformation() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        PaymentCreatedEvent event = new PaymentCreatedEvent(
                this,
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentMethod.CARD,
                com.popcorn.demo.domain.payment.entity.PaymentStatus.READY,
                50000,
                OrderType.RESERVATION,
                1L,
                LocalDateTime.now());

        // 이 메서드는 단순히 로깅만 하므로 예외가 발생하지 않음을 확인
        handler.handlePaymentCreated(event);
    }

    @Test
    void handlePaymentFailedLogsInformation() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        PaymentFailedEvent event = new PaymentFailedEvent(
                this,
                UUID.randomUUID(),
                UUID.randomUUID(),
                PaymentMethod.TRANSFER,
                30000,
                OrderType.PURCHASE,
                2L,
                LocalDateTime.now());

        // 이 메서드는 단순히 로깅만 하므로 예외가 발생하지 않음을 확인
        handler.handlePaymentFailed(event);
    }

    @Test
    void handlePaymentLoggingProcessesSuccessEvent() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-LOGGING-1")
                .paymentId(UUID.randomUUID())
                .totalAmount(75000)
                .paymentKey("test-payment-key")
                .userId(3L)
                .build();

        // 이 메서드는 로깅과 TODO 주석만 있으므로 예외가 발생하지 않음을 확인
        handler.handlePaymentLogging(event);
    }

    @Test
    void handleInventoryDeductionWithUnknownOrderType() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        List<OrderItem> items = List.of(OrderItem.builder().id(UUID.randomUUID()).qty(1).build());

        PaymentSuccessEvent unknownTypeEvent = PaymentSuccessEvent.builder()
                .orderId(orderId)
                .orderNo("O-UNKNOWN")
                .orderType("UNKNOWN_TYPE")
                .orderItems(items)
                .build();

        handler.handleInventoryDeduction(unknownTypeEvent);

        // 알 수 없는 주문 타입은 재고 차감을 하지 않음
        verify(inventoryService, never()).deductInventoryForOrder(Mockito.any(), Mockito.any());
    }

    @Test
    void handleInventoryDeductionWithNullOrderItems() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        PaymentSuccessEvent event = PaymentSuccessEvent.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-NULL-ITEMS")
                .orderType("RESERVATION")
                .orderItems(null)
                .build();

        handler.handleInventoryDeduction(event);

        // null 주문 항목은 재고 차감을 하지 않음
        verify(inventoryService, never()).deductInventoryForOrder(Mockito.any(), Mockito.any());
    }

    @Test
    void handleQrGenerationSkipsNonReservation() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        PaymentSuccessEvent purchaseEvent = PaymentSuccessEvent.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-PURCHASE-QR")
                .orderType("PURCHASE")
                .build();

        PaymentSuccessEvent unknownEvent = PaymentSuccessEvent.builder()
                .orderId(UUID.randomUUID())
                .orderNo("O-UNKNOWN-QR")
                .orderType("UNKNOWN")
                .build();

        handler.handleQrGeneration(purchaseEvent);
        handler.handleQrGeneration(unknownEvent);

        // 예약이 아닌 주문 타입은 QR 생성을 하지 않음
        verify(qrCodeService, never()).issue(Mockito.any());
    }

    @Test
    void handlePaymentApprovedWithEmptyOrderItems() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                this,
                UUID.randomUUID(),
                orderId,
                PaymentMethod.CARD,
                50000,
                OrderType.RESERVATION,
                1L,
                LocalDateTime.now());

        handler.handlePaymentApproved(event);

        // 빈 주문 항목일 때는 재고 차감을 하지 않음
        verify(inventoryService, never()).deductInventoryForOrder(Mockito.any(), Mockito.any());
        // 하지만 예약 주문이므로 QR 생성은 시도함
        verify(qrCodeService).issue(orderId);
    }

    @Test
    void handlePaymentApprovedForPurchaseSkipsQr() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        List<OrderItem> items = List.of(OrderItem.builder().id(UUID.randomUUID()).qty(1).build());
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(items);

        PaymentApprovedEvent event = new PaymentApprovedEvent(
                this,
                UUID.randomUUID(),
                orderId,
                PaymentMethod.CARD,
                40000,
                OrderType.PURCHASE,
                1L,
                LocalDateTime.now());

        handler.handlePaymentApproved(event);

        // 구매 주문은 재고 차감만 하고 QR 생성은 하지 않음
        verify(inventoryService).deductInventoryForOrder(orderId, items);
        verify(qrCodeService, never()).issue(Mockito.any());
    }

    @Test
    void handlePaymentCancelledWithEmptyOrderItems() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository, paymentCancelFailureService);

        UUID orderId = UUID.randomUUID();
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());

        PaymentCancelledEvent event = new PaymentCancelledEvent(
                this,
                UUID.randomUUID(),
                orderId,
                PaymentMethod.CARD,
                30000,
                OrderType.RESERVATION,
                1L,
                LocalDateTime.now());

        handler.handlePaymentCancelled(event);

        // 빈 주문 항목일 때는 재고 복원을 하지 않음
        verify(inventoryService, never()).restoreInventoryForOrder(Mockito.any(), Mockito.any());
    }
}
