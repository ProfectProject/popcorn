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
import com.popcorn.demo.domain.qr.service.QrCodeService;

class PaymentEventHandlerTest {

    @Test
    void handleInventoryDeductionSkipsEmptyItems() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        QrCodeService qrCodeService = Mockito.mock(QrCodeService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository);

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
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository);

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
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository);

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
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository);

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
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, qrCodeService, orderItemRepository);

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
}
