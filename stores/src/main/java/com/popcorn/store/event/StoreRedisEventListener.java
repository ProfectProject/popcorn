package com.popcorn.store.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Store 서비스 Spring Application 이벤트 리스너
 * - Spring Application 이벤트 구독
 * - Store 관련 이벤트 로깅
 *
 * 참고: Redis 이벤트는 StoreRedisStreamListener에서 처리함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreRedisEventListener {


    /**
     * Spring Application 이벤트 수신 (범용)
     */
    @EventListener
    public void handleApplicationEvent(Object event) {
        try {
            String eventType = event.getClass().getSimpleName();

            // Store 관련 이벤트는 더 자세히 로깅
            if (eventType.contains("Store") || eventType.contains("Popup") ||
                eventType.contains("Goods") || eventType.contains("Stock") ||
                eventType.contains("Inventory")) {
                log.info("🌟 [STORES] Store 관련 이벤트 수신 - type: {}, event: {}", eventType, event.toString());
            } else {
                log.debug("🔔 [STORES] Application 이벤트 수신 - type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("🚨 [STORES] Application 이벤트 처리 실패 - event: {}, error: {}",
                    event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }
}
