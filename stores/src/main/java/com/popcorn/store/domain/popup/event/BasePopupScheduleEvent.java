package com.popcorn.store.domain.popup.event;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Getter;

/**
 * 팝업 일정 도메인 이벤트 기본 클래스
 *
 * 기능:
 * - 이벤트 메타데이터 관리
 * - 이벤트 버전 관리
 * - 상관관계 추적 (Correlation ID)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
	@JsonSubTypes.Type(value = PopupScheduleCreatedEvent.class, name = "popup_schedule_created"),
	@JsonSubTypes.Type(value = PopupScheduleUpdatedEvent.class, name = "popup_schedule_updated"),
	@JsonSubTypes.Type(value = PopupScheduleDeletedEvent.class, name = "popup_schedule_deleted")
})
@Getter
public abstract class BasePopupScheduleEvent {

	private final UUID eventId;
	private final UUID popupId;
	private final UUID scheduleId;
	private final String eventType;
	private final LocalDateTime timestamp;
	private final String eventVersion;
	private final UUID correlationId;
	private final Long ownerId;
	private final Map<String, Object> metadata;

	protected BasePopupScheduleEvent(
			UUID popupId,
			UUID scheduleId,
			String eventType,
			Long ownerId,
			Map<String, Object> metadata) {
		this.eventId = UUID.randomUUID();
		this.popupId = popupId;
		this.scheduleId = scheduleId;
		this.eventType = eventType;
		this.timestamp = LocalDateTime.now();
		this.eventVersion = "1.0";
		this.correlationId = UUID.randomUUID();
		this.ownerId = ownerId;
		this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
	}

	protected BasePopupScheduleEvent(UUID popupId, UUID scheduleId, String eventType, Long ownerId) {
		this(popupId, scheduleId, eventType, ownerId, null);
	}

	public EventContext getEventContext() {
		return EventContext.builder()
				.eventId(eventId)
				.correlationId(correlationId)
				.timestamp(timestamp)
				.version(eventVersion)
				.build();
	}

	public <T> T getMetadata(String key, Class<T> type) {
		Object value = metadata.get(key);
		return type.isInstance(value) ? type.cast(value) : null;
	}

	public boolean hasMetadata(String key) {
		return metadata.containsKey(key);
	}

	public String getEventDescription() {
		return String.format("%s[id=%s, popupId=%s, scheduleId=%s, ownerId=%s, timestamp=%s]",
				eventType, eventId, popupId, scheduleId, ownerId, timestamp);
	}

	protected abstract Map<String, Object> getEventPayload();

	@Getter
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
