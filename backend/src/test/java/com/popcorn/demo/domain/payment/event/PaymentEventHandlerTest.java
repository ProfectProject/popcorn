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

class PaymentEventHandlerTest {

    @Test
    void handleInventoryDeductionSkipsEmptyItems() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
    void handlePaymentApprovedProcessesInventory() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
    }

    @Test
    void handlePaymentCancelledRestoresInventory() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
    void handlePaymentLoggingProcessesSuccessEvent() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
    void handlePaymentCancelFailedAddsToFailureQueue() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

        PaymentCancelFailedEvent event = new PaymentCancelFailedEvent(
                this,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "payment_key_123",
                "사용자 취소",
                "네트워크 오류",
                50000,
                LocalDateTime.now(),
                2);

        handler.handlePaymentCancelFailed(event);

        verify(paymentCancelFailureService).addToFailureQueue(event);
    }

    @Test
    void handleInventoryDeductionWithUnknownOrderType() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
    void handlePaymentCreatedLogsInformation() {
        InventoryService inventoryService = Mockito.mock(InventoryService.class);
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
        JpaOrderItemRepository orderItemRepository = Mockito.mock(JpaOrderItemRepository.class);
        PaymentCancelFailureService paymentCancelFailureService = Mockito.mock(PaymentCancelFailureService.class);
        PaymentEventHandler handler = new PaymentEventHandler(inventoryService, orderItemRepository, paymentCancelFailureService);

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
}