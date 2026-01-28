package com.popcorn.store.inventory.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis를 활용해 주문/결제 이벤트의 멱등성을 보장하는 헬퍼.
 * 이벤트 ID 또는 주문 ID+scope를 기준으로 처리 여부를 기록하며,
 * 중복된 이벤트는 다시 처리하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventIdempotencyService {

    private static final Duration EVENT_KEY_TTL = Duration.ofHours(2);
    private static final String EVENT_KEY_FORMAT = "inventory:event:%s:%s";

    private final StringRedisTemplate redisTemplate;

    /**
     * 지정된 scope 내에서 eventId 또는 orderId를 이용해 이벤트 처리 여부를 기록.
     * 이미 기록된 이벤트라면 false를 반환해 중복 처리를 막는다.
     *
     * @param eventId  이벤트 ID (가능하면 사용)
     * @param orderId  주문 ID (eventId가 없을 때 사용)
     * @param scope    처리 대상 그룹 (goods, schedule 등)
     */
    public boolean registerEvent(String eventId, UUID orderId, String scope) {
        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("scope is required for idempotency tracking");
        }
        String identifier = resolveIdentifier(eventId, orderId);
        String key = String.format(EVENT_KEY_FORMAT, scope, identifier);
        Boolean inserted = redisTemplate.opsForValue().setIfAbsent(key, "1", EVENT_KEY_TTL);
        if (Boolean.TRUE.equals(inserted)) {
            log.debug("inventory event 등록 완료 - key={}", key);
            return true;
        }
        log.info("중복 이벤트 감지 - key={}", key);
        return false;
    }

    private String resolveIdentifier(String eventId, UUID orderId) {
        if (StringUtils.hasText(eventId)) {
            return eventId;
        }
        if (orderId != null) {
            return orderId.toString();
        }
        throw new IllegalArgumentException("eventId 또는 orderId 중 하나는 반드시 필요합니다");
    }
}
