package com.popcorn.demo.application.order.usecase;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderDomainService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

	private final OrderDomainService orderDomainService;
	private final FindOrderPort findOrderPort;
	private final SaveOrderPort saveOrderPort;

	@Transactional(transactionManager = "connectionFactoryTransactionManager")
	public Mono<Order> updateStatus(UUID orderId, String status, String reason) {
		log.info("🧾 주문 상태 변경 요청 - 주문ID: {}, 변경상태: {}, 사유: {}", orderId, status, reason);

		return findOrderPort.findById(orderId)
				.switchIfEmpty(Mono.error(OrderException.orderNotFound()))
				.flatMap(order -> {
					OrderStatus currentStatus = order.getStatus();
					if (currentStatus == OrderStatus.CANCELLED) {
						return Mono.error(OrderException.alreadyCanceled());
					}

					OrderStatus newStatus;
					try {
						newStatus = OrderStatus.valueOf(status);
					} catch (IllegalArgumentException ex) {
						return Mono.error(OrderException.invalidRequest());
					}

					if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
						log.warn("❌ 주문 상태 전이 불가 - 주문ID: {}, 현재상태: {}, 요청상태: {}, 사유: {}",
							orderId, currentStatus, newStatus, reason);
						return Mono.error(OrderException.invalidStatusTransition());
					}

					order.setStatus(newStatus);
					return saveOrderPort.save(order)
							.flatMap(savedOrder -> {
								OrderStatusHistory history = OrderStatusHistory.builder()
										.orderId(savedOrder.getId())
										.fromStatus(currentStatus)
										.toStatus(newStatus)
										.reason(reason)
										.changedAt(LocalDateTime.now())
										.build();
								return saveOrderPort.saveStatusHistory(history).thenReturn(savedOrder);
							})
							.doOnSuccess(savedOrder ->
									log.info("✅ 주문 상태 변경 완료 - 주문ID: {}, 이전상태: {}, 변경상태: {}",
										savedOrder.getId(), currentStatus, newStatus));
				});
	}
}
