package com.popcorn.demo.application.order.port.out;

import java.util.UUID;

import reactor.core.publisher.Mono;

/**

	* 주문 처리 포트 (Output Port)

	*

	* Clean Architecture에서 애플리케이션 계층이 인프라 계층에게 요구하는 계약입니다.

	* - 주문 관련 처리 기능 정의

	* - 인프라 계층에서 구현 (Adapter)

	* - 작업들과의 연동

	*/

public interface ProcessOrderPort {



	/**

		* 주문 후처리 작업들을 실행합니다.

		*

		* @param orderId 주문 ID

		* @return 작업 완료 Mono

		*/

	Mono<Void> processOrderPostActions(UUID orderId);



	/**

		* 주문 검증을 실행합니다.

		*

		* @param userId 사용자 ID

		* @param productId 상품 ID

		* @param quantity 수량

		* @return 검증 결과 Mono

		*/

	Mono<Boolean> validateOrder(Long userId, UUID productId, Integer quantity);



	/**

		* 재고 차감을 실행합니다.

		*

		* @param orderId 주문 ID

		* @return 차감 결과 Mono

		*/

	Mono<Boolean> deductStock(UUID orderId);



	/**

		* 결제 처리를 실행합니다.

		*

		* @param orderId 주문 ID

		* @param paymentInfo 결제 정보

		* @return 결제 결과 Mono

		*/

	Mono<Boolean> processPayment(UUID orderId, String paymentInfo);

}
