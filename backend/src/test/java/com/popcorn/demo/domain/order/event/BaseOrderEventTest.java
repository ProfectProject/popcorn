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

	private static final class TestEvent extends BaseOrderEvent {
		TestEvent(UUID orderId, Long userId, Map<String, Object> metadata) {
			super(orderId, "order_test", userId, metadata);
		}

		@Override
		protected Map<String, Object> getEventPayload() {
			return Map.of("payload", true);
		}
	}
}
