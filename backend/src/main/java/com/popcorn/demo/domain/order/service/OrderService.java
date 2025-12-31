package com.popcorn.demo.domain.order.service;

import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**

	* 주문 비동기 처리 서비스

	*

	* 주요 기능:

	* - 주문 후처리 작업 비동기 실행

	* - 외부 시스템 통신 비동기 처리

	* - 이벤트 발행 및 알림 처리

	*/

@Service

public class OrderService {



	private static final Logger log = LoggerFactory.getLogger(OrderService.class);



	/**

		* 주문 생성 후처리 작업

		* - 재고 차감

		* - 알림 발송

		* - 이벤트 발행

		*

		* @param orderId 주문 ID

		* @return 처리 완료 Mono

		*/

	public Mono<Void> processOrderPostActions(UUID orderId) {

		return Mono.delay(Duration.ofMillis(100))
				.doOnNext(ignored -> log.info("주문 {} 재고 차감 완료", orderId))
				.then(Mono.delay(Duration.ofMillis(50))
						.doOnNext(ignored -> log.info("주문 {} 고객 알림 발송 완료", orderId)))
				.then(Mono.delay(Duration.ofMillis(30))
						.doOnNext(ignored -> log.info("주문 {} 이벤트 발행 완료", orderId)))
				.then();

	}



	/**

		* 주문 검증 비동기 처리

		* - 상품 재고 확인

		* - 고객 신용도 확인

		* - 프로모션 유효성 확인

		*

		* @param userId 사용자 ID

		* @param productId 상품 ID

		* @param qty 수량

		* @return 검증 결과 Mono

		*/

	public Mono<Boolean> validateOrderAsync(Long userId, UUID productId, Integer qty) {

		Mono<Boolean> stock = validateStock(qty);
		Mono<Boolean> user = validateCustomer(userId);
		Mono<Boolean> product = validateProduct(productId);

		return Mono.zip(stock, user, product)
				.map(tuple -> tuple.getT1() && tuple.getT2() && tuple.getT3())
				.doOnSuccess(result ->
						log.info("주문 검증 완료 - 사용자: {}, 상품: {}, 결과: {}", userId, productId, result))
				.doOnError(ex ->
						log.error("주문 검증 실패 - 사용자: {}, 상품: {}, 에러: {}", userId, productId, ex.getMessage()));

	}



	/**

		* 결제 처리 비동기 시뮬레이션

		* - 외부 결제 게이트웨이 호출

		* - 결제 결과 처리

		*

		* @param orderId 주문 ID

		* @param amount 결제 금액

		* @return 결제 결과 Mono

		*/

	public Mono<String> processPaymentAsync(UUID orderId, Integer amount) {

		return Mono.delay(Duration.ofMillis(200))
				.map(ignored -> {
					boolean paymentSuccess = Math.random() > 0.1;
					String paymentId = paymentSuccess ? "PAY-" + System.currentTimeMillis() : null;
					log.info("주문 {} 결제 처리 {}", orderId,
							paymentSuccess ? "성공: " + paymentId : "실패");
					return paymentId;
				});

	}



	private Mono<Boolean> validateStock(Integer qty) {
		return Mono.delay(Duration.ofMillis(50))
				.map(ignored -> qty != null && qty > 0);
	}

	private Mono<Boolean> validateCustomer(Long userId) {
		return Mono.delay(Duration.ofMillis(40))
				.map(ignored -> userId != null && userId > 0);
	}

	private Mono<Boolean> validateProduct(UUID productId) {
		return Mono.delay(Duration.ofMillis(30))
				.map(ignored -> productId != null);
	}
}
