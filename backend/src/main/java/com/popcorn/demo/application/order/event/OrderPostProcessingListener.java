package com.popcorn.demo.application.order.event;

import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.popcorn.demo.application.order.port.out.NotifyOrderPort;
import com.popcorn.demo.application.order.port.out.ProcessOrderPort;
import com.popcorn.demo.domain.order.entity.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component

@RequiredArgsConstructor

@Slf4j

public class OrderPostProcessingListener {



	private final ProcessOrderPort processOrderPort;

	private final NotifyOrderPort notifyOrderPort;



	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)

	public void handle(OrderCreatedEvent event) {

		Order order = event.order();

		UUID orderId = order.getId();



		log.info("🧩 주문 후처리 이벤트 처리 시작 - 주문ID: {}", orderId);



		processOrderPort.processOrderPostActions(orderId)
				.doOnError(throwable ->
						log.error("❌ 주문 후처리 작업 실패 - 주문ID: {}, 에러: {}", orderId, throwable.getMessage()))
				.then(notifyOrderPort.notifyOrderCreated(order))
				.doOnSuccess(ignored -> log.info("📣 주문 생성 알림 전송 완료 - 주문ID: {}", orderId))
				.subscribe();

	}

}
