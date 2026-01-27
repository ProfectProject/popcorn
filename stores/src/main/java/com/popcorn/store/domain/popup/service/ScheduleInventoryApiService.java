package com.popcorn.store.domain.popup.service;

import com.popcorn.store.domain.popup.dto.query.response.PopupScheduleCapacity;
import com.popcorn.store.domain.popup.exception.PopupException;
import com.popcorn.store.inventory.redis.InventoryRedisHoldService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleInventoryApiService {

    private static final String SCHEDULE_KEY_FMT = "schedule_avail:{%s}:%s";

    private final InventoryRedisHoldService holdService;
    private final StringRedisTemplate redisTemplate;

    public HoldResult reserve(UUID popupId, UUID scheduleId, int quantity, UUID orderId) {
        if (orderId == null) {
            throw PopupException.missingOrderId();
        }
        String key = buildKey(popupId, scheduleId);
        InventoryRedisHoldService.HoldResult holdResult = holdService.holdSchedule(orderId, popupId, scheduleId, quantity);
        if (!holdResult.isSuccess()) {
            throw PopupException.insufficientReservationCapacity();
        }
        int remaining = readAvailability(key);
        log.info("[SCHEDULE_HOLD] popupId={}, scheduleId={}, qty={}, orderId={}",
                popupId, scheduleId, quantity, orderId);
        return new HoldResult(orderId, quantity, remaining);
    }

    public void release(UUID orderId) {
        if (orderId == null) {
            return;
        }
        holdService.releaseHold(orderId);
    }

    public void releaseQuietly(UUID orderId) {
        try {
            release(orderId);
        } catch (Exception e) {
            log.warn("[SCHEDULE_RELEASE] failed for orderId={}, err={}", orderId, e.getMessage(), e);
        }
    }

    public int available(UUID popupId, UUID scheduleId) {
        return readAvailability(buildKey(popupId, scheduleId));
    }

    private int readAvailability(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String buildKey(UUID popupId, UUID scheduleId) {
        return String.format(SCHEDULE_KEY_FMT, popupId, scheduleId);
    }

    @Getter
    @AllArgsConstructor
    public static class HoldResult {
        private final UUID orderId;
        private final int quantity;
        private final int remaining;
        private final LocalDateTime heldAt = LocalDateTime.now();
    }
}
