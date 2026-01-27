package com.popcorn.store.inventory.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

/**
 * Redis Lua 스크립트를 이용해 재고 HOLD/RELEASE를 원자적으로 수행하는 헬퍼.
 * - HOLD: `hold_stock.lua`에서 availability 키를 차감하고 order별 hold 데이터를 남김.
 * - RELEASE: `release_hold.lua`로 hold 데이터를 읽어서 availability를 복구함.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryRedisHoldService {

    private static final Duration HOLD_TTL = Duration.ofMinutes(30);
    private static final String HOLD_KEY_FORMAT = "hold:%s";

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> holdStockScript;
    private final RedisScript<Long> releaseHoldScript;

    /**
     * 주어진 주문에 대해 availabilityKey에서 quantity만큼 HOLD.
     * 실패 시 재고 부족으로 판단해 false로 반환.
     */
    public boolean holdAvailability(UUID orderId, String availabilityKey, int quantity) {
        if (orderId == null || availabilityKey == null || quantity <= 0) {
            throw new IllegalArgumentException("orderId, availabilityKey, quantity are required");
        }

        try {
            String holdKey = buildHoldKey(orderId);
            Long ttlMs = HOLD_TTL.toMillis();
            Long result = redisTemplate.execute(
                    holdStockScript,
                    Collections.singletonList(availabilityKey),
                    holdKey,
                    String.valueOf(quantity),
                    String.valueOf(ttlMs)
            );
            boolean success = result != null && result == 1L;
            if (!success) {
                log.warn("재고 HOLD 실패 - key: {}, qty: {}, orderId: {}", availabilityKey, quantity, orderId);
            }
            return success;
        } catch (Exception e) {
            log.error("재고 HOLD 스크립트 실행 실패 - key: {}, orderId: {}, error: {}",
                    availabilityKey, orderId, e.getMessage(), e);
            throw new RuntimeException("재고 HOLD 처리 실패", e);
        }
    }

    /**
     * 주문별 HOLD 키를 조회하여 남은 수량을 availability로 되돌리고 HOLD 키를 삭제.
     * 재시도나 실패 복구 시 해당 메서드를 호출하면 Redis 기준 재고가 복구됨.
     */
    public boolean releaseHold(UUID orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required for release");
        }

        try {
            String holdKey = buildHoldKey(orderId);
            Long result = redisTemplate.execute(
                    releaseHoldScript,
                    Collections.singletonList(holdKey)
            );
            boolean released = result != null && result == 1L;
            if (!released) {
                log.info("해당 주문에 HOLD 정보 없음 (이미 해제되었거나 존재하지 않음) - orderId: {}", orderId);
            } else {
                log.info("HOLD 재고 복구 완료 - orderId: {}", orderId);
            }
            return released;
        } catch (Exception e) {
            log.error("HOLD 복구 스크립트 실행 실패 - orderId: {}, error: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("HOLD 복구 처리 실패", e);
        }
    }

    public String buildHoldKey(UUID orderId) {
        return String.format(HOLD_KEY_FORMAT, orderId);
    }
}
