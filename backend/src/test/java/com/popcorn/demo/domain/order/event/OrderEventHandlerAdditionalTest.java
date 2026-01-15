package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.service.OrderNotificationService;
import com.popcorn.demo.domain.order.service.OrderQueryService;
import com.popcorn.demo.domain.payment.service.TossPaymentService;
import com.popcorn.demo.domain.payment.repository.JpaPaymentRepository;
import org.springframework.context.ApplicationEventPublisher;

class OrderEventHandlerAdditionalTest {

	@Test
	@DisplayName("주문 완료 이벤트 처리 시 메트릭이 기록된다")
	void handleOrderCompletedRecordsMetrics() {
		OrderEventMetrics metrics = Mockito.mock(OrderEventMetrics.class);
		OrderEventHandler handler = new OrderEventHandler(
				Mockito.mock(OrderQueryService.class),
				Mockito.mock(OrderNotificationService.class),
				Mockito.mock(OrderEventStore.class),
				metrics,
				Mockito.mock(TossPaymentService.class),
				Mockito.mock(ApplicationEventPublisher.class),
				Mockito.mock(JpaPaymentRepository.class),
				Mockito.mock(ObjectMapper.class));

		OrderCompletedEvent event = new OrderCompletedEvent(
				UUID.randomUUID(),
				1001L,
				UUID.randomUUID(),
				LocalDateTime.now().minusMinutes(20),
				"SYSTEM",
				120000,
				2
		);

		handler.handleOrderCompleted(event);

		verify(metrics).recordEventProcessed("order_completed", event.getServiceQuality());
	}

	@Test
	@DisplayName("결제 이벤트 처리 시 저장과 메트릭을 수행한다")
	void handlePaymentProcessedRecordsMetrics() {
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics metrics = Mockito.mock(OrderEventMetrics.class);
		OrderEventHandler handler = new OrderEventHandler(
				Mockito.mock(OrderQueryService.class),
				Mockito.mock(OrderNotificationService.class),
				eventStore,
				metrics,
				Mockito.mock(TossPaymentService.class),
				Mockito.mock(ApplicationEventPublisher.class),
				Mockito.mock(JpaPaymentRepository.class),
				Mockito.mock(ObjectMapper.class));

		OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
				UUID.randomUUID(),
				1001L,
				"CARD",
				"COMPLETED",
				250000,
				0,
				"PAY-1",
				"TX-1",
				"PG"
		);

		handler.handlePaymentProcessed(event);

		verify(eventStore).saveEvent(event);
		verify(metrics).recordEventProcessed("order_payment_processed", event.getPaymentRisk());
	}

	@Test
	@DisplayName("이벤트 처리 실패 시 오류 메트릭을 기록한다")
	void handleOrderStatusChangedRecordsErrorOnFailure() {
		OrderEventStore eventStore = Mockito.mock(OrderEventStore.class);
		OrderEventMetrics metrics = Mockito.mock(OrderEventMetrics.class);
		OrderEventHandler handler = new OrderEventHandler(
				Mockito.mock(OrderQueryService.class),
				Mockito.mock(OrderNotificationService.class),
				eventStore,
				metrics,
				Mockito.mock(TossPaymentService.class),
				Mockito.mock(ApplicationEventPublisher.class),
				Mockito.mock(JpaPaymentRepository.class),
				Mockito.mock(ObjectMapper.class));

		OrderStatusChangedEvent event = new OrderStatusChangedEvent(
				UUID.randomUUID(),
				1001L,
				OrderStatus.REQUESTED,
				OrderStatus.ACCEPTED,
				"승인",
				"SYSTEM"
		);

		Mockito.doThrow(new RuntimeException("fail")).when(eventStore).saveEvent(event);

		assertThatThrownBy(() -> handler.handleOrderStatusChanged(event))
				.isInstanceOf(RuntimeException.class);

		verify(metrics).recordEventError("order_status_changed", "RuntimeException");
	}
}
