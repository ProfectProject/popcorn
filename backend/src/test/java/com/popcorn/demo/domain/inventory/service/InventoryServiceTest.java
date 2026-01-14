package com.popcorn.demo.domain.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.popcorn.demo.domain.order.entity.OrderItem;

class InventoryServiceTest {

    @Test
    void getCurrentGoodsStockReturnsValue() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        when(jdbcTemplate.query(contains("p_goods_variants"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of(5));

        Integer stock = service.getCurrentGoodsStock(UUID.randomUUID());

        assertThat(stock).isEqualTo(5);
    }

    @Test
    void getCurrentGoodsStockReturnsNullOnError() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
                .thenThrow(new RuntimeException("fail"));

        Integer stock = service.getCurrentGoodsStock(UUID.randomUUID());

        assertThat(stock).isNull();
    }

    @Test
    void deductInventoryForOrderHandlesReservationAndGoods() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID scheduleId = UUID.randomUUID();
        UUID goodsId = UUID.randomUUID();
        OrderItem reservation = OrderItem.builder()
                .id(UUID.randomUUID())
                .sessionOptionId(scheduleId)
                .qty(2)
                .build();
        OrderItem goods = OrderItem.builder()
                .id(UUID.randomUUID())
                .goodsVariantId(goodsId)
                .qty(3)
                .build();

        when(jdbcTemplate.query(contains("p_popup_schedules"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of(5));
        when(jdbcTemplate.query(contains("p_goods_variants"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of(10));
        when(jdbcTemplate.update(contains("p_popup_schedules"), anyInt(), any(UUID.class), anyInt()))
                .thenReturn(1);
        when(jdbcTemplate.update(contains("p_goods_variants"), anyInt(), any(UUID.class), anyInt()))
                .thenReturn(1);

        service.deductInventoryForOrder(UUID.randomUUID(), List.of(reservation, goods));
    }

    @Test
    void deductInventoryForOrderThrowsOnInsufficientCapacity() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID scheduleId = UUID.randomUUID();
        OrderItem reservation = OrderItem.builder()
                .id(UUID.randomUUID())
                .sessionOptionId(scheduleId)
                .qty(2)
                .build();

        when(jdbcTemplate.query(contains("p_popup_schedules"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of(1));

        assertThatThrownBy(() -> service.deductInventoryForOrder(UUID.randomUUID(), List.of(reservation)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void restoreInventoryForOrderIgnoresUpdateFailures() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID scheduleId = UUID.randomUUID();
        UUID goodsId = UUID.randomUUID();
        OrderItem reservation = OrderItem.builder()
                .id(UUID.randomUUID())
                .sessionOptionId(scheduleId)
                .qty(1)
                .build();
        OrderItem goods = OrderItem.builder()
                .id(UUID.randomUUID())
                .goodsVariantId(goodsId)
                .qty(1)
                .build();

        when(jdbcTemplate.update(contains("p_popup_schedules"), anyInt(), any(UUID.class)))
                .thenThrow(new RuntimeException("fail"));
        when(jdbcTemplate.update(contains("p_goods_variants"), anyInt(), any(UUID.class)))
                .thenThrow(new RuntimeException("fail"));

        service.restoreInventoryForOrder(UUID.randomUUID(), List.of(reservation, goods));
    }

    @Test
    void getCurrentGoodsStockReturnsNullWhenNotFound() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of());

        Integer stock = service.getCurrentGoodsStock(UUID.randomUUID());
        assertThat(stock).isNull();
    }

    @Test
    void getCurrentGoodsStockHandlesException() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Database error"));

        Integer stock = service.getCurrentGoodsStock(UUID.randomUUID());
        assertThat(stock).isNull();
    }

    @Test
    void getCurrentScheduleCapacityReturnsNullWhenNotFound() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of());

        Integer capacity = service.getCurrentScheduleCapacity(UUID.randomUUID());
        assertThat(capacity).isNull();
    }

    @Test
    void getCurrentScheduleCapacityHandlesException() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(UUID.class)))
                .thenThrow(new RuntimeException("Database error"));

        Integer capacity = service.getCurrentScheduleCapacity(UUID.randomUUID());
        assertThat(capacity).isNull();
    }

    @Test
    void deductInventoryForOrderWithUnknownItemType() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        OrderItem unknownItem = OrderItem.builder()
                .id(UUID.randomUUID())
                .qty(1)
                .build(); // Neither sessionOptionId nor goodsVariantId set

        service.deductInventoryForOrder(UUID.randomUUID(), List.of(unknownItem));
        // Should log warning but not throw exception
    }

    @Test
    void deductInventoryThrowsWhenGoodsNotFound() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID goodsId = UUID.randomUUID();
        OrderItem goods = OrderItem.builder()
                .id(UUID.randomUUID())
                .goodsVariantId(goodsId)
                .qty(1)
                .build();

        when(jdbcTemplate.query(contains("p_goods_variants"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.deductInventoryForOrder(UUID.randomUUID(), List.of(goods)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

    @Test
    void deductInventoryThrowsWhenScheduleNotFound() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID scheduleId = UUID.randomUUID();
        OrderItem reservation = OrderItem.builder()
                .id(UUID.randomUUID())
                .sessionOptionId(scheduleId)
                .qty(1)
                .build();

        when(jdbcTemplate.query(contains("p_popup_schedules"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.deductInventoryForOrder(UUID.randomUUID(), List.of(reservation)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("스케줄을 찾을 수 없습니다");
    }

    @Test
    void deductInventoryThrowsWhenInsufficientGoodsStock() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID goodsId = UUID.randomUUID();
        OrderItem goods = OrderItem.builder()
                .id(UUID.randomUUID())
                .goodsVariantId(goodsId)
                .qty(10)
                .build();

        when(jdbcTemplate.query(contains("p_goods_variants"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of(5)); // Current stock is 5, but requesting 10

        assertThatThrownBy(() -> service.deductInventoryForOrder(UUID.randomUUID(), List.of(goods)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("상품 재고가 부족합니다");
    }

    @Test
    void deductInventoryThrowsWhenUpdateFails() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        UUID goodsId = UUID.randomUUID();
        OrderItem goods = OrderItem.builder()
                .id(UUID.randomUUID())
                .goodsVariantId(goodsId)
                .qty(1)
                .build();

        when(jdbcTemplate.query(contains("p_goods_variants"), any(RowMapper.class), any(UUID.class)))
                .thenReturn(List.of(5));
        when(jdbcTemplate.update(contains("p_goods_variants"), anyInt(), any(UUID.class), anyInt()))
                .thenReturn(0); // No rows updated

        assertThatThrownBy(() -> service.deductInventoryForOrder(UUID.randomUUID(), List.of(goods)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("상품 재고 차감에 실패했습니다");
    }

    @Test
    void restoreInventoryWithEmptyList() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        service.restoreInventoryForOrder(UUID.randomUUID(), List.of());
        // Should complete without error
    }

    @Test
    void deductInventoryWithEmptyList() {
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
        InventoryService service = new InventoryService(jdbcTemplate);

        service.deductInventoryForOrder(UUID.randomUUID(), List.of());
        // Should complete without error
    }
}
