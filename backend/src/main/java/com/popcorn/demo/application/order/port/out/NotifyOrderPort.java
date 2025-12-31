package com.popcorn.demo.application.order.port.out;

import reactor.core.publisher.Mono;

import com.popcorn.demo.domain.order.entity.Order;

/**

	* 주문 알림 포트 (Output Port)

	*

	* Clean Architecture에서 애플리케이션 계층이 인프라 계층에게 요구하는 계약입니다.

	* - 주문 관련 알림 기능 정의

	* - 인프라 계층에서 구현 (Adapter)

	* - 외부 알림 서비스와의 연동

	*/

public interface NotifyOrderPort {



	/**

		* 주문 생성 알림을 발송합니다.

		*

		* @param order 생성된 주문 엔티티

		*/

	Mono<Void> notifyOrderCreated(Order order);



	/**

		* 주문 상태 변경 알림을 발송합니다.

		*

		* @param order 상태가 변경된 주문 엔티티

		*/

	Mono<Void> notifyOrderStatusChanged(Order order);



	/**

		* 주문 취소 알림을 발송합니다.

		*

		* @param order 취소된 주문 엔티티

		*/

	Mono<Void> notifyOrderCancelled(Order order);

}
