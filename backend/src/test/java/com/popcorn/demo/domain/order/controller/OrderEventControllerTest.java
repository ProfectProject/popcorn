package com.popcorn.demo.domain.order.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import com.popcorn.demo.common.dto.BaseResponse;
import com.popcorn.demo.domain.order.event.BaseOrderEvent;
import com.popcorn.demo.domain.order.event.OrderCancelledEvent;
import com.popcorn.demo.domain.order.event.OrderEventMetrics;
import com.popcorn.demo.domain.order.event.OrderEventStore;
import com.popcorn.demo.domain.order.event.OrderEventStore.EventRecord;
import com.popcorn.demo.domain.order.event.OrderEventStore.EventStoreStats;
import com.popcorn.demo.domain.order.entity.OrderStatus;

class OrderEventControllerTest {

	@Test
	@DisplayName("Event endpoints return success responses")
	void eventEndpointsReturnSuccessResponses() {
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics eventMetrics = Mockito.mock(OrderEventMetrics.class);
		OrderEventController controller = new OrderEventController(eventStore, eventMetrics);

		UUID orderId = UUID.randomUUID();
		EventRecord record = EventRecord.builder()
				.eventId(UUID.randomUUID())
				.orderId(orderId)
				.eventType("order_created")
				.eventVersion("1.0")
				.correlationId(UUID.randomUUID())
				.userId(1001L)
				.timestamp(LocalDateTime.now())
				.eventData("{}")
				.metadata("{}")
				.build();
		List<EventRecord> records = List.of(record);
		when(eventStore.getEventStream(orderId)).thenReturn(records);
		when(eventStore.getEventStream(Mockito.eq(orderId), Mockito.any(), Mockito.any())).thenReturn(records);
		when(eventStore.getEventsByType("order_created")).thenReturn(records);
		when(eventStore.getRecentEvents(2)).thenReturn(records);
		when(eventStore.replayEvents(orderId)).thenReturn(List.of(new OrderCancelledEvent(
				orderId, 1001L, OrderStatus.PAID, "test", "1001", 1000
		)));

		OrderEventMetrics.EventMetrics metrics = OrderEventMetrics.EventMetrics.builder()
				.totalEventsProcessed(10)
				.totalErrorsOccurred(1)
				.errorRate(10.0)
				.mostProcessedEventType("order_created")
				.mostCommonErrorType("none")
				.eventProcessedCounts(Map.of())
				.eventErrorCounts(Map.of())
				.priorityProcessedCounts(Map.of())
				.errorTypeCounts(Map.of())
				.lastResetTime(LocalDateTime.now())
				.build();
		OrderEventMetrics.EventTypeMetrics typeMetrics = OrderEventMetrics.EventTypeMetrics.builder()
				.eventType("order_created")
				.processedCount(5)
				.errorCount(0)
				.errorRate(0.0)
				.isHealthy(true)
				.build();
		when(eventMetrics.getMetrics()).thenReturn(metrics);
		when(eventMetrics.getEventTypeMetrics("order_created")).thenReturn(typeMetrics);
		when(eventMetrics.getMetricsSummary()).thenReturn("summary");
		when(eventMetrics.isHealthy()).thenReturn(true);

		EventStoreStats stats = EventStoreStats.builder()
				.totalEvents(3)
				.totalOrderStreams(1)
				.eventTypeCounts(Map.of("order_created", 3L))
				.oldestEventTime(LocalDateTime.now().minusDays(1))
				.newestEventTime(LocalDateTime.now())
				.build();
		when(eventStore.getStatistics()).thenReturn(stats);

		ResponseEntity<BaseResponse<List<EventRecord>>> streamResponse = controller.getEventStream(orderId);
		assertThat(streamResponse.getBody().getData()).hasSize(1);

		ResponseEntity<BaseResponse<List<EventRecord>>> rangeResponse = controller.getEventStreamByTimeRange(
				orderId, LocalDateTime.now().minusHours(1), LocalDateTime.now());
		assertThat(rangeResponse.getBody().getData()).hasSize(1);

		ResponseEntity<BaseResponse<List<EventRecord>>> typeResponse = controller.getEventsByType("order_created");
		assertThat(typeResponse.getBody().getData()).hasSize(1);

		ResponseEntity<BaseResponse<List<EventRecord>>> recentResponse = controller.getRecentEvents(2);
		assertThat(recentResponse.getBody().getData()).hasSize(1);

		ResponseEntity<BaseResponse<List<BaseOrderEvent>>> replayResponse = controller.replayEvents(orderId);
		assertThat(replayResponse.getBody().getData()).hasSize(1);

		ResponseEntity<BaseResponse<OrderEventMetrics.EventMetrics>> metricsResponse = controller.getEventMetrics();
		assertThat(metricsResponse.getBody().getData().getTotalEventsProcessed()).isEqualTo(10);

		ResponseEntity<BaseResponse<OrderEventMetrics.EventTypeMetrics>> typeMetricsResponse =
				controller.getEventTypeMetrics("order_created");
		assertThat(typeMetricsResponse.getBody().getData().getProcessedCount()).isEqualTo(5);

		ResponseEntity<String> summaryResponse = controller.getMetricsSummary();
		assertThat(summaryResponse.getBody()).isEqualTo("summary");

		ResponseEntity<BaseResponse<EventStoreStats>> statsResponse = controller.getEventStoreStats();
		assertThat(statsResponse.getBody().getData().getTotalEvents()).isEqualTo(3);

		ResponseEntity<BaseResponse<String>> cleanupResponse = controller.cleanupEventStore(LocalDateTime.now());
		assertThat(cleanupResponse.getBody().getData()).isNotBlank();
		verify(eventStore).cleanup(Mockito.any());

		ResponseEntity<BaseResponse<String>> resetResponse = controller.resetMetrics();
		assertThat(resetResponse.getBody().getData()).isNotBlank();
		verify(eventMetrics).resetMetrics();

		ResponseEntity<BaseResponse<String>> healthResponse = controller.healthCheck();
		assertThat(healthResponse.getBody().getData()).isNotBlank();
	}

	@Test
	@DisplayName("Event endpoints propagate failures as runtime exceptions")
	void eventEndpointsThrowOnFailure() {
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics eventMetrics = Mockito.mock(OrderEventMetrics.class);
		OrderEventController controller = new OrderEventController(eventStore, eventMetrics);

		UUID orderId = UUID.randomUUID();
		Mockito.when(eventStore.getEventStream(orderId)).thenThrow(new RuntimeException("fail"));

		org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getEventStream(orderId))
				.isInstanceOf(RuntimeException.class);

		Mockito.when(eventMetrics.isHealthy()).thenReturn(false);
		Mockito.when(eventStore.getStatistics()).thenReturn(
				EventStoreStats.builder()
						.totalEvents(0)
						.totalOrderStreams(0)
						.eventTypeCounts(Map.of())
						.oldestEventTime(null)
						.newestEventTime(null)
						.build());

		org.assertj.core.api.Assertions.assertThatThrownBy(controller::healthCheck)
				.isInstanceOf(RuntimeException.class);
	}
}
