package com.popcorn.demo.domain.order.event;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.popcorn.demo.domain.order.entity.Order;
import com.popcorn.demo.domain.order.service.OrderNotificationService;
import com.popcorn.demo.domain.order.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPostProcessingListener {



	private final OrderService orderService;
	private final OrderNotificationService orderNotificationService;



	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(OrderCreatedEvent event) {
		Order order = event.order();
		UUID orderId = order.getId();

		log.info("🧩 주문 후처리 이벤트 처리 시작 - 주문ID: {}", orderId);
		try {
			orderService.processOrderPostActions(orderId);
			orderNotificationService.notifyOrderCreated(order);
			log.info("📣 주문 생성 알림 전송 완료 - 주문ID: {}", orderId);
		} catch (RuntimeException ex) {
			log.error("❌ 주문 후처리 작업 실패 - 주문ID: {}, 에러: {}", orderId, ex.getMessage(), ex);
		}
	}

}
