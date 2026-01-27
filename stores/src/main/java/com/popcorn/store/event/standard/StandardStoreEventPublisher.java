package com.popcorn.store.event.standard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 표준 Store 이벤트 발행자
 * 새로운 표준 이벤트 구조로 Redis Stream에 이벤트를 발행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StandardStoreEventPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    // 새로운 표준 Stream 이름
    private static final String STANDARD_POPUP_EVENTS_STREAM = "standard-popup-events";

    /**
     * 표준 팝업 생성 이벤트 발행
     */
    public void publishPopupCreatedEvent(StandardPopupCreatedEvent event) {
        publishStandardEvent(event, event.toStreamMap());
    }

    /**
     * 표준 팝업 상태 변경 이벤트 발행
     */
    public void publishPopupStatusUpdatedEvent(StandardPopupStatusUpdatedEvent event) {
        publishStandardEvent(event, event.toStreamMap());
    }

    /**
     * 표준 이벤트 Redis Stream 발행
     */
    private void publishStandardEvent(StandardBaseEvent event, java.util.Map<String, String> eventData) {
        try {
            log.info("🚀 [STANDARD-STORE] 표준 이벤트 발행 시작 - eventType: {}, eventId: {}",
                    event.getEventType(), event.getEventId());

            // StringRecord로 Redis Stream에 발행
            StringRecord record = StreamRecords.string(eventData)
                    .withStreamKey(STANDARD_POPUP_EVENTS_STREAM);

            String recordId = redisTemplate.opsForStream().add(record).getValue();

            log.info("✅ [STANDARD-STORE] 표준 이벤트 발행 완료 - eventType: {}, eventId: {}, recordId: {}",
                    event.getEventType(), event.getEventId(), recordId);

        } catch (Exception e) {
            log.error("❌ [STANDARD-STORE] 표준 이벤트 발행 실패 - eventType: {}, eventId: {}, error: {}",
                    event.getEventType(), event.getEventId(), e.getMessage(), e);
        }
    }
}