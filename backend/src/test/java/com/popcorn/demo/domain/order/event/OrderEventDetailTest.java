package com.popcorn.demo.domain.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.popcorn.demo.domain.order.entity.OrderStatus;

class OrderEventDetailTest {

	@Test
	@DisplayName("완료 이벤트는 서비스 품질과 우선순위를 계산한다")
	void completedEventCalculations() {
		OrderCompletedEvent quickHighValue = new OrderCompletedEvent(
				UUID.randomUUID(),
				1001L,
				UUID.randomUUID(),
				LocalDateTime.now().minusMinutes(20),
				"SYSTEM",
				120000,
				2
		);

		assertThat(quickHighValue.isQuickService()).isTrue();
		assertThat(quickHighValue.isHighValueOrder()).isTrue();
		assertThat(quickHighValue.getReviewRequestPriority()).isEqualTo("HIGH");
		assertThat(quickHighValue.getServiceQuality()).isEqualTo("EXCELLENT");

		OrderCompletedEvent delayed = new OrderCompletedEvent(
				UUID.randomUUID(),
				1001L,
				UUID.randomUUID(),
				LocalDateTime.now().minusMinutes(120),
				"SYSTEM",
				10000,
				1
		);

		assertThat(delayed.isDelayedService()).isTrue();
		assertThat(delayed.getReviewRequestPriority()).isEqualTo("LOW");
		assertThat(delayed.getServiceQuality()).isEqualTo("NEEDS_IMPROVEMENT");
	}

	@Test
	@DisplayName("결제 이벤트는 위험도와 정산 우선순위를 계산한다")
	void paymentEventCalculations() {
		OrderPaymentProcessedEvent event = new OrderPaymentProcessedEvent(
				UUID.randomUUID(),
				1001L,
				"CASH",
				"COMPLETED",
				600000,
				10000,
				"PAY-1",
				"TX-1",
				"PG"
		);

		assertThat(event.isSuccessfulPayment()).isTrue();
		assertThat(event.hasDiscount()).isTrue();
		assertThat(event.getPaymentRisk()).isEqualTo("HIGH");
		assertThat(event.getSettlementPriority()).isEqualTo("HIGH");
		assertThat(event.getDiscountRate()).isGreaterThan(0.0);
	}

	@Test
	@DisplayName("상태 변경 이벤트는 타입과 설명을 계산한다")
	void statusChangedEventCalculations() {
		OrderStatusChangedEvent event = new OrderStatusChangedEvent(
				UUID.randomUUID(),
				1001L,
				OrderStatus.REQUESTED,
				OrderStatus.ACCEPTED,
				"승인",
				"1001"
		);

		assertThat(event.isStatusProgression()).isTrue();
		assertThat(event.isReversibleChange()).isTrue();
		assertThat(event.isCriticalStatusChange()).isTrue();
		assertThat(event.requiresCustomerNotification()).isTrue();
		assertThat(event.getStatusChangeDescription()).contains("요청됨");
	}
}
