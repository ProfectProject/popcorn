package com.popcorn.demo.domain.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.dto.CreateOrderRequest;
import com.popcorn.demo.domain.order.dto.OrderCreatedDto;
import com.popcorn.demo.domain.order.dto.OrderItemRequest;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(OrderExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("Create order returns 201 with payload")
    void createOrder_Success() throws Exception {
        OrderCreatedDto.OrderItemDto itemDto = new OrderCreatedDto.OrderItemDto(
                10L,
                "RESERVATION",
                2,
                14500,
                29000
        );

        OrderCreatedDto createdDto = new OrderCreatedDto(
                1L,
                "O20251230-000001",
                "RESERVATION",
                "REQUESTED",
                1L,
                2L,
                29000,
                LocalDateTime.of(2025, 12, 30, 14, 15),
                LocalDateTime.of(2025, 12, 30, 13, 10),
                List.of(itemDto)
        );

        when(orderService.createOrder(eq(1001L), any(CreateOrderRequest.class), isNull()))
                .thenReturn(createdDto);

        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("RESERVATION")
                .storeId(1L)
                .productId(2L)
                .items(List.of(OrderItemRequest.builder()
                        .orderItemType("RESERVATION")
                        .sessionId(101L)
                        .optionId(201L)
                        .qty(2)
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/orders/{userId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.orderType").value("RESERVATION"))
                .andExpect(jsonPath("$.data.items[0].orderItemType").value("RESERVATION"));
    }

    @Test
    @DisplayName("Validation failure returns 400")
    void createOrder_ValidationFail_ReturnsBadRequest() throws Exception {
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .orderType("RESERVATION")
                .storeId(1L)
                .productId(2L)
                .items(List.of())
                .build();

        mockMvc.perform(post("/api/v1/orders/{userId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("items")));
    }

    @Test
    @DisplayName("Validation failure returns 400 for invalid qty")
    void createOrder_InvalidQty_ReturnsBadRequest() throws Exception {
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .orderType("RESERVATION")
                .storeId(1L)
                .productId(2L)
                .items(List.of(OrderItemRequest.builder()
                        .orderItemType("RESERVATION")
                        .sessionId(101L)
                        .optionId(201L)
                        .qty(0)
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/orders/{userId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("qty")));
    }

    @Test
    @DisplayName("Missing order type returns 400")
    void createOrder_MissingOrderType_ReturnsBadRequest() throws Exception {
        CreateOrderRequest invalidRequest = CreateOrderRequest.builder()
                .orderType("")
                .storeId(1L)
                .productId(2L)
                .items(List.of(OrderItemRequest.builder()
                        .orderItemType("RESERVATION")
                        .sessionId(101L)
                        .optionId(201L)
                        .qty(1)
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/orders/{userId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("orderType")));
    }

    

    @Test
    @DisplayName("Service exception returns 400")
    void createOrder_ServiceThrows_ReturnsBadRequest() throws Exception {
        when(orderService.createOrder(eq(1001L), any(CreateOrderRequest.class), any()))
                .thenThrow(OrderException.invalidRequest());

        CreateOrderRequest request = CreateOrderRequest.builder()
                .orderType("RESERVATION")
                .storeId(1L)
                .productId(2L)
                .items(List.of(OrderItemRequest.builder()
                        .orderItemType("RESERVATION")
                        .sessionId(101L)
                        .optionId(201L)
                        .qty(2)
                        .build()))
                .build();

        mockMvc.perform(post("/api/v1/orders/{userId}", 1001L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
