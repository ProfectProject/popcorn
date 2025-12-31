package com.popcorn.demo.application.order.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.common.dto.ResponseCode;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderDomainService;

import lombok.extern.slf4j.Slf4j;

@ExtendWith(MockitoExtension.class)
@Slf4j
class UpdateOrderStatusUseCaseTest {

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
		Order order = Order.builder()
				.id(1L)
				.status(OrderStatus.REQUESTED)
				.build();

		when(findOrderPort.findById(1L)).thenReturn(Optional.of(order));
		when(saveOrderPort.save(order)).thenReturn(order);

		Order result = updateOrderStatusUseCase.updateStatus(1L, "OWNER_ACCEPTED", "approved");

		assertThat(result.getStatus()).isEqualTo(OrderStatus.OWNER_ACCEPTED);

		ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
		verify(saveOrderPort).saveStatusHistory(historyCaptor.capture());
		assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(OrderStatus.OWNER_ACCEPTED);
		assertThat(historyCaptor.getValue().getReason()).isEqualTo("approved");
		log.info("✅ 주문 상태 변경 유스케이스 성공 테스트 완료");
	}

	@Test
	void updateStatus_orderNotFound() {
		log.info("🧪 주문 상태 변경 유스케이스 주문 없음 테스트 시작");
		when(findOrderPort.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> updateOrderStatusUseCase.updateStatus(1L, "OWNER_ACCEPTED", "reason"))
				.isInstanceOf(OrderException.class)
				.extracting("responseCode")
				.isEqualTo(ResponseCode.ORDER_NOT_FOUND);
		log.info("✅ 주문 상태 변경 유스케이스 주문 없음 테스트 완료");
	}

	@Test
	void updateStatus_alreadyCanceled() {
		log.info("🧪 주문 상태 변경 유스케이스 이미 취소 테스트 시작");
		Order order = Order.builder()
				.id(1L)
				.status(OrderStatus.CANCELLED)
				.build();

		when(findOrderPort.findById(1L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> updateOrderStatusUseCase.updateStatus(1L, "COMPLETED", "reason"))
				.isInstanceOf(OrderException.class)
				.extracting("responseCode")
				.isEqualTo(ResponseCode.ALREADY_CANCELED);
		log.info("✅ 주문 상태 변경 유스케이스 이미 취소 테스트 완료");
	}

	@Test
	void updateStatus_invalidTransition() {
		log.info("🧪 주문 상태 변경 유스케이스 전이 오류 테스트 시작");
		Order order = Order.builder()
				.id(1L)
				.status(OrderStatus.READY)
				.build();

		when(findOrderPort.findById(1L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> updateOrderStatusUseCase.updateStatus(1L, "OWNER_ACCEPTED", "reason"))
				.isInstanceOf(OrderException.class)
				.extracting("responseCode")
				.isEqualTo(ResponseCode.INVALID_STATUS_TRANSITION);
		log.info("✅ 주문 상태 변경 유스케이스 전이 오류 테스트 완료");
	}

	@Test
	void updateStatus_invalidStatusValue() {
		log.info("🧪 주문 상태 변경 유스케이스 상태값 오류 테스트 시작");
		Order order = Order.builder()
				.id(1L)
				.status(OrderStatus.REQUESTED)
				.build();

		when(findOrderPort.findById(1L)).thenReturn(Optional.of(order));

		assertThatThrownBy(() -> updateOrderStatusUseCase.updateStatus(1L, "NOT_A_STATUS", "reason"))
				.isInstanceOf(OrderException.class)
				.extracting("responseCode")
				.isEqualTo(ResponseCode.INVALID_REQUEST);
		log.info("✅ 주문 상태 변경 유스케이스 상태값 오류 테스트 완료");
	}
}
