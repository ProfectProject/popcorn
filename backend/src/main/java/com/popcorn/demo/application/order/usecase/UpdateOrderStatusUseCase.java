package com.popcorn.demo.application.order.usecase;

import java.time.LocalDateTime;

import org.springframework.transaction.annotation.Transactional;

import com.popcorn.demo.application.order.port.out.FindOrderPort;
import com.popcorn.demo.application.order.port.out.SaveOrderPort;
import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.entity.OrderStatus;
import com.popcorn.demo.domain.order.entity.OrderStatusHistory;
import com.popcorn.demo.domain.order.exception.OrderException;
import com.popcorn.demo.domain.order.service.OrderDomainService;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

	private final OrderDomainService orderDomainService;
	private final FindOrderPort findOrderPort;
	private final SaveOrderPort saveOrderPort;

	@Transactional
	public Order updateStatus(Long orderId, String status, String reason) {
		log.info("🧾 주문 상태 변경 요청 - 주문ID: {}, 변경상태: {}, 사유: {}", orderId, status, reason);
		// 상태 변경 전에 주문을 조회하고 전이 가능 여부를 검증합니다.
		Order order = findOrderPort.findById(orderId)
				.orElseThrow(OrderException::orderNotFound);

		OrderStatus currentStatus = order.getStatus();
		if (currentStatus == OrderStatus.CANCELLED) {
			throw OrderException.alreadyCanceled();
		}

		OrderStatus newStatus;
		try {
			newStatus = OrderStatus.valueOf(status);
		} catch (IllegalArgumentException ex) {
			throw OrderException.invalidRequest();
		}

		if (!orderDomainService.canChangeStatus(currentStatus, newStatus)) {
			throw OrderException.invalidStatusTransition();
		}

		order.setStatus(newStatus);
		Order savedOrder = saveOrderPort.save(order);

		// 상태 변경 이력을 저장해 감사 추적이 가능하게 합니다.
		OrderStatusHistory history = OrderStatusHistory.builder()
				.orderId(savedOrder.getId())
				.fromStatus(currentStatus)
				.toStatus(newStatus)
				.reason(reason)
				.changedAt(LocalDateTime.now())
				.build();
		saveOrderPort.saveStatusHistory(history);

		log.info("✅ 주문 상태 변경 완료 - 주문ID: {}, 이전상태: {}, 변경상태: {}", savedOrder.getId(), currentStatus, newStatus);
		return savedOrder;
	}
}
