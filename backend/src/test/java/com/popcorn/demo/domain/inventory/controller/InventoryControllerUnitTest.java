package com.popcorn.demo.domain.inventory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.inventory.service.InventoryService;

class InventoryControllerUnitTest {

    @Mock
    private InventoryService inventoryService;

    private InventoryController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new InventoryController(inventoryService);
    }

    @Test
    void getGoodsStockReturnsSoldOutStatus() {
        // Given
        UUID goodsVariantId = UUID.randomUUID();
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(0);

        // When
        ResponseEntity<BaseResponse<InventoryController.GoodsStockResponse>> response =
                controller.getGoodsStock(goodsVariantId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getCurrentStock()).isEqualTo(0);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("SOLD_OUT");
    }

    @Test
    void getGoodsStockReturnsLowStockStatus() {
        // Given
        UUID goodsVariantId = UUID.randomUUID();
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(3);

        // When
        ResponseEntity<BaseResponse<InventoryController.GoodsStockResponse>> response =
                controller.getGoodsStock(goodsVariantId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getCurrentStock()).isEqualTo(3);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("LOW_STOCK");
    }

    @Test
    void getGoodsStockReturnsNormalStatus() {
        // Given
        UUID goodsVariantId = UUID.randomUUID();
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(15);

        // When
        ResponseEntity<BaseResponse<InventoryController.GoodsStockResponse>> response =
                controller.getGoodsStock(goodsVariantId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getCurrentStock()).isEqualTo(15);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("NORMAL");
    }

    @Test
    void getGoodsStockReturnsSufficientStatus() {
        // Given
        UUID goodsVariantId = UUID.randomUUID();
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(50);

        // When
        ResponseEntity<BaseResponse<InventoryController.GoodsStockResponse>> response =
                controller.getGoodsStock(goodsVariantId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getCurrentStock()).isEqualTo(50);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("SUFFICIENT");
    }

    @Test
    void getGoodsStockThrowsWhenStockIsNull() {
        // Given
        UUID goodsVariantId = UUID.randomUUID();
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> controller.getGoodsStock(goodsVariantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

    @Test
    void getScheduleCapacityReturnsFullStatus() {
        // Given
        UUID scheduleId = UUID.randomUUID();
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(0);

        // When
        ResponseEntity<BaseResponse<InventoryController.ScheduleCapacityResponse>> response =
                controller.getScheduleCapacity(scheduleId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getRemainingCapacity()).isEqualTo(0);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("FULL");
    }

    @Test
    void getScheduleCapacityReturnsAlmostFullStatus() {
        // Given
        UUID scheduleId = UUID.randomUUID();
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(2);

        // When
        ResponseEntity<BaseResponse<InventoryController.ScheduleCapacityResponse>> response =
                controller.getScheduleCapacity(scheduleId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getRemainingCapacity()).isEqualTo(2);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("ALMOST_FULL");
    }

    @Test
    void getScheduleCapacityReturnsNormalStatus() {
        // Given
        UUID scheduleId = UUID.randomUUID();
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(8);

        // When
        ResponseEntity<BaseResponse<InventoryController.ScheduleCapacityResponse>> response =
                controller.getScheduleCapacity(scheduleId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getRemainingCapacity()).isEqualTo(8);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("NORMAL");
    }

    @Test
    void getScheduleCapacityReturnsAvailableStatus() {
        // Given
        UUID scheduleId = UUID.randomUUID();
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(25);

        // When
        ResponseEntity<BaseResponse<InventoryController.ScheduleCapacityResponse>> response =
                controller.getScheduleCapacity(scheduleId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getRemainingCapacity()).isEqualTo(25);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void getScheduleCapacityThrowsWhenCapacityIsNull() {
        // Given
        UUID scheduleId = UUID.randomUUID();
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> controller.getScheduleCapacity(scheduleId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("스케줄을 찾을 수 없습니다");
    }

    @Test
    void getGoodsStockWithBoundaryValues() {
        // Test boundary values for status classification
        UUID goodsVariantId = UUID.randomUUID();

        // Test exact boundary for LOW_STOCK (5)
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(5);
        ResponseEntity<BaseResponse<InventoryController.GoodsStockResponse>> response =
                controller.getGoodsStock(goodsVariantId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("LOW_STOCK");

        // Test boundary for NORMAL (20)
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(20);
        response = controller.getGoodsStock(goodsVariantId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("NORMAL");

        // Test boundary for SUFFICIENT (21)
        when(inventoryService.getCurrentGoodsStock(goodsVariantId)).thenReturn(21);
        response = controller.getGoodsStock(goodsVariantId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("SUFFICIENT");
    }

    @Test
    void getScheduleCapacityWithBoundaryValues() {
        // Test boundary values for capacity status classification
        UUID scheduleId = UUID.randomUUID();

        // Test exact boundary for ALMOST_FULL (3)
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(3);
        ResponseEntity<BaseResponse<InventoryController.ScheduleCapacityResponse>> response =
                controller.getScheduleCapacity(scheduleId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("ALMOST_FULL");

        // Test boundary for NORMAL (10)
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(10);
        response = controller.getScheduleCapacity(scheduleId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("NORMAL");

        // Test boundary for AVAILABLE (11)
        when(inventoryService.getCurrentScheduleCapacity(scheduleId)).thenReturn(11);
        response = controller.getScheduleCapacity(scheduleId);
        assertThat(response.getBody().getData().getStatus()).isEqualTo("AVAILABLE");
    }
}