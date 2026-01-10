package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderStatus;

class OrderEventTypesTest {

	@Test
	@DisplayName("주문 취소 이벤트 비즈니스 로직을 검증한다")
	void cancelledEventBusinessLogic() {
		OrderCancelledEvent event = new OrderCancelledEvent(
				UUID.randomUUID(),
				1001L,
				OrderStatus.PAID,
				"고객 취소",
				"1001",
				12000
		);

		assertThat(event.isRefundRequired()).isTrue();
		assertThat(event.requiresCompensation()).isTrue();
		assertThat(event.isCustomerInitiated()).isTrue();
		assertThat(event.getCancellationSeverity()).isEqualTo("HIGH");
		assertThat(event.getEventPayload()).containsKeys("orderId", "severity");
		assertThat(event.getCancellationDescription()).contains("환불");
	}

	@Test
	@DisplayName("주문 상태 변경 이벤트 로직을 검증한다")
	void statusChangedEventLogic() {
		OrderStatusChangedEvent event = new OrderStatusChangedEvent(
				UUID.randomUUID(),
				1001L,
				OrderStatus.REQUESTED,
				OrderStatus.ACCEPTED,
				"승인",
				"OWNER"
		);

		assertThat(event.isStatusProgression()).isTrue();
		assertThat(event.isReversibleChange()).isTrue();
		assertThat(event.isCriticalStatusChange()).isTrue();
		assertThat(event.requiresCustomerNotification()).isTrue();
		assertThat(event.getStatusChangeDescription()).contains("요청됨");
	}

	@Test
	@DisplayName("주문 완료 이벤트 로직을 검증한다")
	void completedEventLogic() {
		LocalDateTime createdAt = LocalDateTime.now().minusMinutes(20);
		OrderCompletedEvent event = new OrderCompletedEvent(
				UUID.randomUUID(),
				1001L,
				UUID.randomUUID(),
				createdAt,
				"SYSTEM",
				60000,
				3
		);

		assertThat(event.isHighValueOrder()).isTrue();
		assertThat(event.isQuickService()).isTrue();
		assertThat(event.getReviewRequestPriority()).isEqualTo("HIGH");
		assertThat(event.getAccrualPoints()).isPositive();
		assertThat(event.getServiceQuality()).isEqualTo("EXCELLENT");
		assertThat(event.getCompletionDescription()).contains("주문 완료");
	}

	@Test
	@DisplayName("결제 처리 이벤트 로직을 검증한다")
	void paymentProcessedEventLogic() {
		OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
				UUID.randomUUID(),
				1001L,
				"CARD",
				"COMPLETED",
				200000,
				20000,
				"PAY-1",
				"TX-1",
				"TEST-PG"
		);

		assertThat(event.isSuccessfulPayment()).isTrue();
		assertThat(event.hasDiscount()).isTrue();
		assertThat(event.getDiscountRate()).isGreaterThan(0);
		assertThat(event.getPaymentRisk()).isEqualTo("MEDIUM");
		assertThat(event.getSettlementPriority()).isEqualTo("HIGH");
		assertThat(event.getPaymentDescription()).contains("결제 처리");
	}
}
