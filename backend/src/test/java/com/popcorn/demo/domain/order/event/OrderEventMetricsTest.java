package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderEventMetricsTest {

	@Test
	@DisplayName("이벤트 처리/오류 기록이 메트릭에 반영된다")
	void metricsCaptureProcessedAndErrors() {
		OrderEventMetrics metrics = new OrderEventMetrics();

		metrics.recordEventProcessed("order_created", "HIGH");
		metrics.recordEventProcessed("order_created", "HIGH");
		metrics.recordEventError("order_created", "RuntimeException");

		OrderEventMetrics.EventMetrics snapshot = metrics.getMetrics();

		assertThat(snapshot.getTotalEventsProcessed()).isEqualTo(2);
		assertThat(snapshot.getTotalErrorsOccurred()).isEqualTo(1);
		assertThat(snapshot.getEventProcessedCounts().get("order_created")).isEqualTo(2);
		assertThat(snapshot.getEventErrorCounts().get("order_created")).isEqualTo(1);
		assertThat(snapshot.getPriorityProcessedCounts().get("HIGH")).isEqualTo(2);
		assertThat(snapshot.getErrorTypeCounts().get("RuntimeException")).isEqualTo(1);
		assertThat(snapshot.getErrorRate()).isGreaterThan(0);
	}

	@Test
	@DisplayName("이벤트 타입 메트릭과 리셋이 동작한다")
	void eventTypeMetricsAndReset() {
		OrderEventMetrics metrics = new OrderEventMetrics();

		metrics.recordEventProcessed("order_cancelled", "MEDIUM");
		metrics.recordEventError("order_cancelled", "IllegalState");

		OrderEventMetrics.EventTypeMetrics typeMetrics = metrics.getEventTypeMetrics("order_cancelled");
		assertThat(typeMetrics.getProcessedCount()).isEqualTo(1);
		assertThat(typeMetrics.getErrorCount()).isEqualTo(1);
		assertThat(typeMetrics.isHealthy()).isFalse();

		assertThat(metrics.isHealthy()).isFalse();
		assertThat(metrics.getMetricsSummary()).contains("order_cancelled");

		metrics.resetMetrics();
		OrderEventMetrics.EventMetrics resetSnapshot = metrics.getMetrics();
		assertThat(resetSnapshot.getTotalEventsProcessed()).isZero();
		assertThat(resetSnapshot.getTotalErrorsOccurred()).isZero();
	}
}
