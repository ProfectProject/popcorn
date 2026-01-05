package com.popcorn.demo.domain.order.event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 이벤트 저장소
 *
 * 이벤트 소싱 패턴 구현:
 * - 모든 이벤트의 저장 및 조회
 * - 이벤트 재생(replay) 기능
 * - 이벤트 스트림 관리
 * - 스냅샷 기능 (향후 확장)
 */
@Service
@RequiredArgsConstructor
public class OrderEventStore {

    private static final Logger log = LoggerFactory.getLogger(OrderEventStore.class);

    private final ObjectMapper objectMapper;

    // 메모리 기반 이벤트 저장소 (실제 운영에서는 데이터베이스 사용)
    private final Map<UUID, List<EventRecord>> eventStreams = new ConcurrentHashMap<>();
    private final List<EventRecord> globalEventLog = new CopyOnWriteArrayList<>();

    /**
     * 이벤트 저장
     */
    public void saveEvent(BaseOrderEvent event) {
        try {
            EventRecord record = createEventRecord(event);

            // 주문별 이벤트 스트림에 추가
            eventStreams.computeIfAbsent(event.getOrderId(), k -> new CopyOnWriteArrayList<>())
                       .add(record);

            // 전역 이벤트 로그에 추가
            globalEventLog.add(record);

            log.debug("📦 이벤트 저장 완료 - 타입: {}, 주문ID: {}, 이벤트ID: {}",
                    event.getEventType(), event.getOrderId(), event.getEventId());

        } catch (Exception e) {
            log.error("❌ 이벤트 저장 실패 - 타입: {}, 주문ID: {}",
                    event.getEventType(), event.getOrderId(), e);
            throw new EventStoreException("이벤트 저장 중 오류 발생", e);
        }
    }

    /**
     * 특정 주문의 이벤트 스트림 조회
     */
    public List<EventRecord> getEventStream(UUID orderId) {
        List<EventRecord> events = eventStreams.get(orderId);
        return events != null ? List.copyOf(events) : List.of();
    }

    /**
     * 특정 주문의 이벤트 스트림 조회 (시간 범위)
     */
    public List<EventRecord> getEventStream(UUID orderId, LocalDateTime fromTime, LocalDateTime toTime) {
        return getEventStream(orderId).stream()
                .filter(record -> {
                    LocalDateTime eventTime = record.getTimestamp();
                    return !eventTime.isBefore(fromTime) && !eventTime.isAfter(toTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 이벤트 타입 조회
     */
    public List<EventRecord> getEventsByType(String eventType) {
        return globalEventLog.stream()
                .filter(record -> eventType.equals(record.getEventType()))
                .collect(Collectors.toList());
    }

    /**
     * 특정 이벤트 타입 조회 (시간 범위)
     */
    public List<EventRecord> getEventsByType(String eventType, LocalDateTime fromTime, LocalDateTime toTime) {
        return getEventsByType(eventType).stream()
                .filter(record -> {
                    LocalDateTime eventTime = record.getTimestamp();
                    return !eventTime.isBefore(fromTime) && !eventTime.isAfter(toTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * 전체 이벤트 로그 조회 (최근 순)
     */
    public List<EventRecord> getRecentEvents(int limit) {
        return globalEventLog.stream()
                .sorted((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 이벤트 재생 (특정 주문)
     */
    public List<BaseOrderEvent> replayEvents(UUID orderId) {
        log.info("🔄 이벤트 재생 시작 - 주문ID: {}", orderId);

        List<EventRecord> eventStream = getEventStream(orderId);
        List<BaseOrderEvent> replayedEvents = eventStream.stream()
                .map(this::deserializeEvent)
                .collect(Collectors.toList());

        log.info("✅ 이벤트 재생 완료 - 주문ID: {}, 이벤트 수: {}",
                orderId, replayedEvents.size());

        return replayedEvents;
    }

    /**
     * 이벤트 스토어 통계
     */
    public EventStoreStats getStatistics() {
        Map<String, Long> eventTypeCounts = globalEventLog.stream()
                .collect(Collectors.groupingBy(
                    EventRecord::getEventType,
                    Collectors.counting()
                ));

        return EventStoreStats.builder()
                .totalEvents(globalEventLog.size())
                .totalOrderStreams(eventStreams.size())
                .eventTypeCounts(eventTypeCounts)
                .oldestEventTime(getOldestEventTime())
                .newestEventTime(getNewestEventTime())
                .build();
    }

    /**
     * 이벤트 스토어 정리 (오래된 이벤트 제거)
     */
    public void cleanup(LocalDateTime beforeTime) {
        log.info("🧹 이벤트 스토어 정리 시작 - 기준시간: {}", beforeTime);

        int removedCount = 0;

        // 전역 로그에서 제거할 항목 계산
        long globalRemovedCount = globalEventLog.stream()
                .filter(record -> record.getTimestamp().isBefore(beforeTime))
                .count();
        globalEventLog.removeIf(record -> record.getTimestamp().isBefore(beforeTime));
        removedCount += (int) globalRemovedCount;

        // 각 주문 스트림에서 제거
        for (List<EventRecord> stream : eventStreams.values()) {
            long streamRemovedCount = stream.stream()
                    .filter(record -> record.getTimestamp().isBefore(beforeTime))
                    .count();
            stream.removeIf(record -> record.getTimestamp().isBefore(beforeTime));
            removedCount += (int) streamRemovedCount;
        }

        // 빈 스트림 제거
        eventStreams.values().removeIf(List::isEmpty);

        log.info("✅ 이벤트 스토어 정리 완료 - 제거된 이벤트: {}개", removedCount);
    }

    // ================ 내부 메서드들 ================

    private EventRecord createEventRecord(BaseOrderEvent event) throws JsonProcessingException {
        return EventRecord.builder()
                .eventId(event.getEventId())
                .orderId(event.getOrderId())
                .eventType(event.getEventType())
                .eventVersion(event.getEventVersion())
                .correlationId(event.getCorrelationId())
                .userId(event.getUserId())
                .timestamp(event.getTimestamp())
                .eventData(objectMapper.writeValueAsString(event.getEventPayload()))
                .metadata(objectMapper.writeValueAsString(event.getMetadata()))
                .build();
    }

    private BaseOrderEvent deserializeEvent(EventRecord record) {
        try {
            // 이벤트 타입별 역직렬화 로직
            // 실제 구현에서는 Jackson의 @JsonTypeInfo를 활용하거나
            // 별도의 EventFactory를 사용할 수 있음
            log.debug("이벤트 역직렬화: 타입={}, ID={}", record.getEventType(), record.getEventId());

            // 현재는 기본 구현으로 대체 (실제로는 완전한 역직렬화 필요)
            return createBasicEvent(record);

        } catch (Exception e) {
            log.error("❌ 이벤트 역직렬화 실패 - ID: {}, 타입: {}",
                    record.getEventId(), record.getEventType(), e);
            throw new EventStoreException("이벤트 역직렬화 실패", e);
        }
    }

    private BaseOrderEvent createBasicEvent(EventRecord record) {
        // 기본 이벤트 객체 생성 (실제 구현에서는 타입별로 완전한 객체 복원)
        return new BaseOrderEvent(record.getOrderId(), record.getEventType(), record.getUserId()) {
            @Override
            protected Map<String, Object> getEventPayload() {
                return Map.of("reconstructed", true);
            }
        };
    }

    private LocalDateTime getOldestEventTime() {
        return globalEventLog.stream()
                .map(EventRecord::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    private LocalDateTime getNewestEventTime() {
        return globalEventLog.stream()
                .map(EventRecord::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    // ================ 내부 클래스들 ================

    /**
     * 저장된 이벤트 레코드
     */
    @Getter
    public static class EventRecord {
        private final UUID eventId;
        private final UUID orderId;
        private final String eventType;
        private final String eventVersion;
        private final UUID correlationId;
        private final Long userId;
        private final LocalDateTime timestamp;
        private final String eventData;
        private final String metadata;

        private EventRecord(Builder builder) {
            this.eventId = builder.eventId;
            this.orderId = builder.orderId;
            this.eventType = builder.eventType;
            this.eventVersion = builder.eventVersion;
            this.correlationId = builder.correlationId;
            this.userId = builder.userId;
            this.timestamp = builder.timestamp;
            this.eventData = builder.eventData;
            this.metadata = builder.metadata;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private UUID eventId;
            private UUID orderId;
            private String eventType;
            private String eventVersion;
            private UUID correlationId;
            private Long userId;
            private LocalDateTime timestamp;
            private String eventData;
            private String metadata;

            public Builder eventId(UUID eventId) { this.eventId = eventId; return this; }
            public Builder orderId(UUID orderId) { this.orderId = orderId; return this; }
            public Builder eventType(String eventType) { this.eventType = eventType; return this; }
            public Builder eventVersion(String eventVersion) { this.eventVersion = eventVersion; return this; }
            public Builder correlationId(UUID correlationId) { this.correlationId = correlationId; return this; }
            public Builder userId(Long userId) { this.userId = userId; return this; }
            public Builder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }
            public Builder eventData(String eventData) { this.eventData = eventData; return this; }
            public Builder metadata(String metadata) { this.metadata = metadata; return this; }

            public EventRecord build() { return new EventRecord(this); }
        }
    }

    /**
     * 이벤트 스토어 통계
     */
    @Getter
    public static class EventStoreStats {
        private final long totalEvents;
        private final long totalOrderStreams;
        private final Map<String, Long> eventTypeCounts;
        private final LocalDateTime oldestEventTime;
        private final LocalDateTime newestEventTime;

        private EventStoreStats(Builder builder) {
            this.totalEvents = builder.totalEvents;
            this.totalOrderStreams = builder.totalOrderStreams;
            this.eventTypeCounts = Map.copyOf(builder.eventTypeCounts);
            this.oldestEventTime = builder.oldestEventTime;
            this.newestEventTime = builder.newestEventTime;
        }

        public static Builder builder() { return new Builder(); }

        public static class Builder {
            private long totalEvents;
            private long totalOrderStreams;
            private Map<String, Long> eventTypeCounts = Map.of();
            private LocalDateTime oldestEventTime;
            private LocalDateTime newestEventTime;

            public Builder totalEvents(long totalEvents) { this.totalEvents = totalEvents; return this; }
            public Builder totalOrderStreams(long totalOrderStreams) { this.totalOrderStreams = totalOrderStreams; return this; }
            public Builder eventTypeCounts(Map<String, Long> eventTypeCounts) { this.eventTypeCounts = eventTypeCounts; return this; }
            public Builder oldestEventTime(LocalDateTime oldestEventTime) { this.oldestEventTime = oldestEventTime; return this; }
            public Builder newestEventTime(LocalDateTime newestEventTime) { this.newestEventTime = newestEventTime; return this; }

            public EventStoreStats build() { return new EventStoreStats(this); }
        }
    }

    /**
     * 이벤트 스토어 예외
     */
    public static class EventStoreException extends RuntimeException {
        public EventStoreException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
