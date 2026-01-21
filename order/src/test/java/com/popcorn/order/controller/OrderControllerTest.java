package com.popcorn.order.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.order.dto.response.CreateOrderResponse;
import com.popcorn.order.service.OrderCommandService;

/**
 * 자바 초보자용 컨트롤러 테스트
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderCommandService orderCommandService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 주문생성_API_테스트() throws Exception {
        // Given
        CreateOrderResponse response = CreateOrderResponse.builder()
                .orderId(UUID.randomUUID())
                .orderNo("ORD-123456")
                .build();

        when(orderCommandService.createOrder(any())).thenReturn(response);

        String requestBody = """
                {
                    "userId": 1,
                    "popupId": "550e8400-e29b-41d4-a716-446655440000",
                    "orderType": "RESERVATION",
                    "items": []
                }
                """;

        // When & Then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

}