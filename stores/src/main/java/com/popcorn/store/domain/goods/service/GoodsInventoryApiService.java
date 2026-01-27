package com.popcorn.store.domain.goods.service;

import com.popcorn.store.domain.goods.dto.GoodsStockResponse;
import com.popcorn.store.domain.goods.exception.GoodsException;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService.GoodsHoldItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoodsInventoryApiService {

    private final InventoryRedisHoldService holdService;
    private final StringRedisTemplate redisTemplate;

    public HoldResult reserve(UUID popupId, UUID goodsId, int quantity, UUID orderId) {
        List<GoodsHoldItem> items = List.of(new GoodsHoldItem(goodsId, quantity));
        String availabilityKey = buildAvailabilityKey(popupId, goodsId);
        InventoryRedisHoldService.HoldResult holdResult = holdService.holdGoods(orderId, popupId, items);
        if (!holdResult.isSuccess()) {
            throw GoodsException.insufficientStock();
        }
        int remaining = readAvailability(availabilityKey);
        log.info("[API_HOLD] popupId={}, goodsId={}, qty={}, orderId={}", popupId, goodsId, quantity, orderId);
        return new HoldResult(orderId, quantity, remaining);
    }

    public void release(UUID orderId) {
        holdService.releaseHold(orderId);
    }

    public void releaseQuietly(UUID orderId) {
        try {
            if (orderId != null) {
                holdService.releaseHold(orderId);
            }
        } catch (Exception e) {
            log.warn("[API_RELEASE] orderId={} release failed: {}", orderId, e.getMessage(), e);
        }
    }

    public GoodsStockResponse currentStock(UUID popupId, UUID goodsId, int reservationStock) {
        String availabilityKey = buildAvailabilityKey(popupId, goodsId);
        int remaining = readAvailability(availabilityKey);
        return GoodsStockResponse.builder()
                .goodsId(goodsId)
                .stock(remaining)
                .reservationStock(reservationStock)
                .build();
    }

    private int readAvailability(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String buildAvailabilityKey(UUID popupId, UUID goodsId) {
        return String.format("goods_avail:{%s}:%s", popupId, goodsId);
    }

    @Getter
    @AllArgsConstructor
    public static class HoldResult {
        private final UUID orderId;
        private final int quantity;
        private final int remaining;
    }
}
