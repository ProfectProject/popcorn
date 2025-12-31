package com.popcorn.demo.application.order.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderDomainService;

@ExtendWith(MockitoExtension.class)
class UpdateOrderStatusUseCaseTest {

	private static final Logger log = LoggerFactory.getLogger(UpdateOrderStatusUseCaseTest.class);

	@Mock
	private FindOrderPort findOrderPort;

	@Mock
	private SaveOrderPort saveOrderPort;

	@Spy
	private OrderDomainService orderDomainService = new OrderDomainService();

	@InjectMocks
	private UpdateOrderStatusUseCase updateOrderStatusUseCase;

	@Test
	void updateStatus_success() {
		log.info("🧪 주문 상태 변경 유스케이스 성공 테스트 시작");
		UUID orderId = UUID.randomUUID();
		Order order = Order.builder()
				.id(orderId)
				.status(OrderStatus.REQUESTED)
				.build();

		when(findOrderPort.findById(orderId)).thenReturn(Mono.just(order));
		when(saveOrderPort.save(order)).thenReturn(Mono.just(order));
		when(saveOrderPort.saveStatusHistory(org.mockito.ArgumentMatchers.any())).thenReturn(Mono.empty());

		StepVerifier.create(updateOrderStatusUseCase.updateStatus(orderId, "OWNER_ACCEPTED", "approved"))
				.assertNext(result -> assertThat(result.getStatus()).isEqualTo(OrderStatus.OWNER_ACCEPTED))
				.verifyComplete();

		ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
		verify(saveOrderPort).saveStatusHistory(historyCaptor.capture());
		assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.OWNER_ACCEPTED);
		assertThat(historyCaptor.getValue().getReason()).isEqualTo("approved");
		log.info("✅ 주문 상태 변경 유스케이스 성공 테스트 완료");
	}

	@Test
	void updateStatus_orderNotFound() {
		log.info("🧪 주문 상태 변경 유스케이스 주문 없음 테스트 시작");
		UUID orderId = UUID.randomUUID();
		when(findOrderPort.findById(orderId)).thenReturn(Mono.empty());

		StepVerifier.create(updateOrderStatusUseCase.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
				.expectErrorSatisfies(ex -> {
					assertThat(ex).isInstanceOf(OrderException.class);
					assertThat(((OrderException) ex).getResponseCode()).isEqualTo(ResponseCode.ORDER_NOT_FOUND);
				})
				.verify();
		log.info("✅ 주문 상태 변경 유스케이스 주문 없음 테스트 완료");
	}

	@Test
	void updateStatus_alreadyCanceled() {
		log.info("🧪 주문 상태 변경 유스케이스 이미 취소 테스트 시작");
		UUID orderId = UUID.randomUUID();
		Order order = Order.builder()
				.id(orderId)
				.status(OrderStatus.CANCELLED)
				.build();

		when(findOrderPort.findById(orderId)).thenReturn(Mono.just(order));

		StepVerifier.create(updateOrderStatusUseCase.updateStatus(orderId, "COMPLETED", "reason"))
				.expectErrorSatisfies(ex -> {
					assertThat(ex).isInstanceOf(OrderException.class);
					assertThat(((OrderException) ex).getResponseCode()).isEqualTo(ResponseCode.ALREADY_CANCELED);
				})
				.verify();
		log.info("✅ 주문 상태 변경 유스케이스 이미 취소 테스트 완료");
	}

	@Test
	void updateStatus_invalidTransition() {
		log.info("🧪 주문 상태 변경 유스케이스 전이 오류 테스트 시작");
		UUID orderId = UUID.randomUUID();
		Order order = Order.builder()
				.id(orderId)
				.status(OrderStatus.READY)
				.build();

		when(findOrderPort.findById(orderId)).thenReturn(Mono.just(order));

		StepVerifier.create(updateOrderStatusUseCase.updateStatus(orderId, "OWNER_ACCEPTED", "reason"))
				.expectErrorSatisfies(ex -> {
					assertThat(ex).isInstanceOf(OrderException.class);
					assertThat(((OrderException) ex).getResponseCode()).isEqualTo(ResponseCode.INVALID_STATUS_TRANSITION);
				})
				.verify();
		log.info("✅ 주문 상태 변경 유스케이스 전이 오류 테스트 완료");
	}

	@Test
	void updateStatus_invalidStatusValue() {
		log.info("🧪 주문 상태 변경 유스케이스 상태값 오류 테스트 시작");
		UUID orderId = UUID.randomUUID();
		Order order = Order.builder()
				.id(orderId)
				.status(OrderStatus.REQUESTED)
				.build();

		when(findOrderPort.findById(orderId)).thenReturn(Mono.just(order));

		StepVerifier.create(updateOrderStatusUseCase.updateStatus(orderId, "NOT_A_STATUS", "reason"))
				.expectErrorSatisfies(ex -> {
					assertThat(ex).isInstanceOf(OrderException.class);
					assertThat(((OrderException) ex).getResponseCode()).isEqualTo(ResponseCode.INVALID_REQUEST);
				})
				.verify();
		log.info("✅ 주문 상태 변경 유스케이스 상태값 오류 테스트 완료");
	}
}
