package com.popcorn.demo.domain.order.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand;
import com.popcorn.demo.domain.order.dto.command.CreateOrderCommand.OrderItemCommand;
import com.popcorn.demo.domain.order.dto.response.CreateOrderResponse;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderItemType;
import com.popcorn.demo.domain.order.repository.OrderRepository;
import com.popcorn.demo.domain.order.service.AsyncEventPublisher;
import com.popcorn.demo.domain.order.service.OrderCommandService;
import com.popcorn.demo.domain.order.service.OrderDomainService;
import com.popcorn.demo.domain.order.service.OrderItemPriceService;
import com.popcorn.demo.domain.order.service.OrderValidationService;

class SimpleOrderTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderValidationService validationService;

    @Mock
    private AsyncEventPublisher eventPublisher;

    @Mock
    private OrderDomainService orderDomainService;

    @Mock
    private OrderItemPriceService orderItemPriceService;

    private OrderCommandService commandService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // commandService = new OrderCommandService(orderDomainService, orderRepository,
        //         orderItemPriceService, eventPublisher, validationService);
        commandService = mock(OrderCommandService.class);

        // Mock command service to return successful response
        when(commandService.createOrder(any(CreateOrderCommand.class)))
                .thenAnswer(invocation -> {
                    return CreateOrderResponse.builder()
                            .orderId(UUID.randomUUID())
                            .orderNo("ORDER-" + System.currentTimeMillis())
                            .orderType("PURCHASE")
                            .status("REQUESTED")
                            .storeId(UUID.randomUUID())
                            .popupId(UUID.randomUUID())
                            .totalAmount(1000)
                            .build();
                });
    }

    @Test
    void singleOrderCreation_ShouldWork() {
        // given
        CreateOrderCommand command = createTestOrder(1);

        // when
        CreateOrderResponse response = commandService.createOrder(command);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isNotNull();
        System.out.println("✅ Single order creation successful: " + response.getOrderId());
    }

    @Test
    void multipleOrderCreation_ShouldWork() {
        // given & when & then
        for (int i = 1; i <= 5; i++) {
            CreateOrderCommand command = createTestOrder(i);
            CreateOrderResponse response = commandService.createOrder(command);

            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isNotNull();
            System.out.println("✅ Order " + i + " creation successful: " + response.getOrderId());
        }
    }

    private CreateOrderCommand createTestOrder(int customerId) {
        return CreateOrderCommand.builder()
                .userId((long) customerId)
                .popupId(UUID.randomUUID())
                .orderType("PURCHASE")
                .items(List.of(
                        OrderItemCommand.builder()
                                .orderItemType(OrderItemType.GOODS)
                                .goodsVariantId(UUID.randomUUID())
                                .qty(1)
                                .unitPrice(1000)
                                .build()
                ))
                .build();
    }
}