package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseOrderEventTest {

	@Test
	@DisplayName("Event context and metadata are accessible")
	void eventContextAndMetadata() {
		UUID orderId = UUID.randomUUID();
		TestEvent event = new TestEvent(orderId, 1001L, Map.of("key", "value"));

		BaseOrderEvent.EventContext context = event.getEventContext();
		assertThat(context.getEventId()).isNotNull();
		assertThat(context.getCorrelationId()).isNotNull();
		assertThat(context.getTimestamp()).isNotNull();
		assertThat(context.getVersion()).isEqualTo("1.0");

		assertThat(event.hasMetadata("key")).isTrue();
		assertThat(event.getMetadata("key", String.class)).isEqualTo("value");
		assertThat(event.getEventDescription()).contains("order_test");
		assertThat(event.toString()).contains("order_test");
	}

	@Test
	@DisplayName("Event store exception preserves cause")
	void eventStoreExceptionKeepsCause() {
		RuntimeException cause = new RuntimeException("cause");
		OrderEventStore.EventStoreException exception =
				new OrderEventStore.EventStoreException("message", cause);

		assertThat(exception.getCause()).isEqualTo(cause);
		assertThat(exception.getMessage()).isEqualTo("message");
	}

	@Test
	@DisplayName("Event created without metadata works correctly")
	void eventWithoutMetadata() {
		UUID orderId = UUID.randomUUID();
		TestEvent event = new TestEvent(orderId, 1001L, null);

		assertThat(event.hasMetadata("nonexistent")).isFalse();
		assertThat(event.getMetadata("nonexistent", String.class)).isNull();
		assertThat(event.getMetadata()).isEmpty();
	}

	@Test
	@DisplayName("Metadata type casting returns null for wrong type")
	void metadataTypeCasting() {
		UUID orderId = UUID.randomUUID();
		TestEvent event = new TestEvent(orderId, 1001L, Map.of("number", 123, "text", "hello"));

		assertThat(event.getMetadata("number", Integer.class)).isEqualTo(123);
		assertThat(event.getMetadata("number", String.class)).isNull(); // Wrong type
		assertThat(event.getMetadata("text", String.class)).isEqualTo("hello");
		assertThat(event.getMetadata("text", Integer.class)).isNull(); // Wrong type
	}

	@Test
	@DisplayName("Event context builder pattern works correctly")
	void eventContextBuilder() {
		UUID eventId = UUID.randomUUID();
		UUID correlationId = UUID.randomUUID();

		BaseOrderEvent.EventContext context = BaseOrderEvent.EventContext.builder()
				.eventId(eventId)
				.correlationId(correlationId)
				.timestamp(java.time.LocalDateTime.now())
				.version("2.0")
				.build();

		assertThat(context.getEventId()).isEqualTo(eventId);
		assertThat(context.getCorrelationId()).isEqualTo(correlationId);
		assertThat(context.getVersion()).isEqualTo("2.0");
	}

	@Test
	@DisplayName("Event created with simplified constructor works")
	void eventWithSimplifiedConstructor() {
		UUID orderId = UUID.randomUUID();
		SimpleTestEvent event = new SimpleTestEvent(orderId, 1001L);

		assertThat(event.getOrderId()).isEqualTo(orderId);
		assertThat(event.getUserId()).isEqualTo(1001L);
		assertThat(event.getEventType()).isEqualTo("simple_test");
		assertThat(event.getMetadata()).isEmpty();
	}

	private static final class SimpleTestEvent extends BaseOrderEvent {
		SimpleTestEvent(UUID orderId, Long userId) {
			super(orderId, "simple_test", userId);
		}

		@Override
		public Map<String, Object> getEventPayload() {
			return Map.of("simple", true);
		}
	}

	private static final class TestEvent extends BaseOrderEvent {
		TestEvent(UUID orderId, Long userId, Map<String, Object> metadata) {
			super(orderId, "order_test", userId, metadata);
		}

		@Override
		public Map<String, Object> getEventPayload() {
			return Map.of("payload", true);
		}
	}
}
