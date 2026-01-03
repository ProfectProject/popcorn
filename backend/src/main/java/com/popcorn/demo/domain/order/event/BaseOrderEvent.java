package com.popcorn.demo.domain.order.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 주문 도메인 이벤트의 기본 클래스
 *
 * 기능:
 * - 이벤트 메타데이터 관리
 * - 이벤트 버전 관리
 * - 상관관계 추적 (Correlation ID)
 * - 이벤트 재생 및 디버깅 지원
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "order_created"),
    @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "order_status_changed"),
    @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "order_cancelled"),
    @JsonSubTypes.Type(value = OrderCompletedEvent.class, name = "order_completed"),
    @JsonSubTypes.Type(value = OrderPaymentProcessedEvent.class, name = "order_payment_processed")
})
public abstract class BaseOrderEvent {

    private final UUID eventId;            // 이벤트 고유 ID
    private final UUID orderId;            // 주문 ID
    private final String eventType;        // 이벤트 타입
    private final LocalDateTime timestamp;  // 이벤트 발생 시간
    private final String eventVersion;     // 이벤트 스키마 버전
    private final UUID correlationId;     // 상관관계 ID (요청 추적용)
    private final Long userId;             // 사용자 ID
    private final Map<String, Object> metadata; // 추가 메타데이터

    protected BaseOrderEvent(UUID orderId, String eventType, Long userId, Map<String, Object> metadata) {
        this.eventId = UUID.randomUUID();
        this.orderId = orderId;
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.eventVersion = "1.0"; // 현재는 고정, 향후 동적 관리 가능
        this.correlationId = generateCorrelationId();
        this.userId = userId;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    protected BaseOrderEvent(UUID orderId, String eventType, Long userId) {
        this(orderId, eventType, userId, null);
    }

    // ================ Getters ================

    public UUID getEventId() { return eventId; }
    public UUID getOrderId() { return orderId; }
    public String getEventType() { return eventType; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getEventVersion() { return eventVersion; }
    public UUID getCorrelationId() { return correlationId; }
    public Long getUserId() { return userId; }
    public Map<String, Object> getMetadata() { return metadata; }

    // ================ Event Context ================

    /**
     * 이벤트가 발생한 컨텍스트 정보 반환
     */
    public EventContext getEventContext() {
        return EventContext.builder()
                .eventId(eventId)
                .correlationId(correlationId)
                .timestamp(timestamp)
                .version(eventVersion)
                .build();
    }

    /**
     * 특정 메타데이터 값 조회
     */
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * 메타데이터 존재 여부 확인
     */
    public boolean hasMetadata(String key) {
        return metadata.containsKey(key);
    }

    // ================ Helper Methods ================

    private UUID generateCorrelationId() {
        // 현재는 새로운 UUID 생성, 향후 MDC나 Request Context에서 가져올 수 있음
        return UUID.randomUUID();
    }

    /**
     * 이벤트 설명 생성 (로깅 및 디버깅용)
     */
    public String getEventDescription() {
        return String.format("%s[id=%s, orderId=%s, userId=%s, timestamp=%s]",
                eventType, eventId, orderId, userId, timestamp);
    }

    /**
     * 이벤트를 JSON 형태로 직렬화할 때 포함할 공통 필드들
     */
    protected abstract Map<String, Object> getEventPayload();

    // ================ 내부 클래스 ================

    /**
     * 이벤트 컨텍스트 정보
     */
    public static class EventContext {
        private final UUID eventId;
        private final UUID correlationId;
        private final LocalDateTime timestamp;
        private final String version;

        private EventContext(Builder builder) {
            this.eventId = builder.eventId;
            this.correlationId = builder.correlationId;
            this.timestamp = builder.timestamp;
            this.version = builder.version;
        }

        public UUID getEventId() { return eventId; }
        public UUID getCorrelationId() { return correlationId; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getVersion() { return version; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private UUID eventId;
            private UUID correlationId;
            private LocalDateTime timestamp;
            private String version;

            public Builder eventId(UUID eventId) {
                this.eventId = eventId;
                return this;
            }

            public Builder correlationId(UUID correlationId) {
                this.correlationId = correlationId;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public EventContext build() {
                return new EventContext(this);
            }
        }
    }

    @Override
    public String toString() {
        return getEventDescription();
    }
}